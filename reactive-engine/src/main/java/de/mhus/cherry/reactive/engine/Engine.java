package de.mhus.cherry.reactive.engine;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import de.mhus.cherry.reactive.model.activity.AActivity;
import de.mhus.cherry.reactive.model.activity.AElement;
import de.mhus.cherry.reactive.model.activity.APool;
import de.mhus.cherry.reactive.model.activity.AStartPoint;
import de.mhus.cherry.reactive.model.activity.ASwimlane;
import de.mhus.cherry.reactive.model.annotations.ActivityDescription;
import de.mhus.cherry.reactive.model.annotations.Trigger;
import de.mhus.cherry.reactive.model.annotations.Trigger.TYPE;
import de.mhus.cherry.reactive.model.engine.AaaProvider;
import de.mhus.cherry.reactive.model.engine.ContextRecipient;
import de.mhus.cherry.reactive.model.engine.EElement;
import de.mhus.cherry.reactive.model.engine.EPool;
import de.mhus.cherry.reactive.model.engine.EProcess;
import de.mhus.cherry.reactive.model.engine.EngineConst;
import de.mhus.cherry.reactive.model.engine.PCase;
import de.mhus.cherry.reactive.model.engine.PCase.STATE_CASE;
import de.mhus.cherry.reactive.model.engine.PCaseInfo;
import de.mhus.cherry.reactive.model.engine.PEngine;
import de.mhus.cherry.reactive.model.engine.PNode;
import de.mhus.cherry.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.cherry.reactive.model.engine.PNode.TYPE_NODE;
import de.mhus.cherry.reactive.model.engine.PNodeInfo;
import de.mhus.cherry.reactive.model.engine.ProcessProvider;
import de.mhus.cherry.reactive.model.engine.Result;
import de.mhus.cherry.reactive.model.engine.RuntimeNode;
import de.mhus.cherry.reactive.model.engine.StorageProvider;
import de.mhus.cherry.reactive.model.errors.TaskException;
import de.mhus.cherry.reactive.model.errors.TechnicalException;
import de.mhus.cherry.reactive.model.migrate.Migrator;
import de.mhus.cherry.reactive.model.util.CloseActivity;
import de.mhus.cherry.reactive.model.util.InactiveStartPoint;
import de.mhus.lib.core.IProperties;
import de.mhus.lib.core.MCast;
import de.mhus.lib.core.MCollection;
import de.mhus.lib.core.MLog;
import de.mhus.lib.core.MProperties;
import de.mhus.lib.core.MString;
import de.mhus.lib.core.MThread;
import de.mhus.lib.core.MTimeInterval;
import de.mhus.lib.core.util.MUri;
import de.mhus.lib.core.util.MutableUri;
import de.mhus.lib.core.util.SoftHashMap;
import de.mhus.lib.errors.MException;
import de.mhus.lib.errors.NotFoundException;
import de.mhus.lib.errors.UsageException;

public class Engine extends MLog {

	private StorageProvider storage;
	private StorageProvider archive;
	private ProcessProvider processProvider;
	private SoftHashMap<UUID,PCase> caseCache = new SoftHashMap<>();
	private SoftHashMap<UUID,PNode> nodeCache = new SoftHashMap<>();
	private HashSet<UUID> running = new HashSet<>();
	private EngineConfiguration config;

	public Engine(EngineConfiguration config) {
		this.config = config;
		storage = config.storage;
		archive = config.archive;
		if (config.listener == null) config.listener = EngineListenerUtil.createQuietListener();
		processProvider = config.processProvider;
		
		try {
			config.persistent = storage.loadEngine();
		} catch (NotFoundException | IOException e) {
			log().i(e);
		}
		if (config.persistent == null) {
			log().i("Initial new engine persistence");
			config.persistent = new PEngine();
		}
		if (config.parameters != null) {
			config.persistent.getParameters().putAll(config.parameters);
			try {
				storage.saveEngine(config.persistent);
			} catch (IOException e) {
				log().e(e);
			}
		}

	}
	
// ---
	
	public void step() throws IOException, NotFoundException {
		execute();
		cleanup();
	}
	
	public int execute() throws IOException, NotFoundException {
		
		int doneCnt = 0;
		
		// Init
		long now = System.currentTimeMillis();

		// SCHEDULED NODES
		config.listener.doStep("scheduled");
		for (PNodeInfo nodeId : storage.getScheduledFlowNodes(STATE_NODE.SCHEDULED, now)) {
			PNode node = getFlowNode(nodeId.getId());
			// set state back to ready
			config.listener.setScheduledToRunning(node);
			node.setState(STATE_NODE.RUNNING);
			saveFlowNode(null, node, null);
		}

		// READY NODES
		config.listener.doStep("execute");
		boolean parallel = MCast.toboolean(config.persistent.getParameters().get(EngineConst.ENGINE_EXECUTE_PARALLEL), true);
		Result<PNodeInfo> result = storage.getScheduledFlowNodes(STATE_NODE.RUNNING, now);
		if (parallel) {
			int maxThreads = MCast.toint(config.persistent.getParameters().get(EngineConst.ENGINE_EXECUTE_MAX_THREADS), 10);
			LinkedList<FlowNodeExecutor> threads = new LinkedList<>();
			for (PNodeInfo nodeId : result) {
				synchronized (running) {
					if (running.contains(nodeId)) continue;
				}
				PNode node = getFlowNode(nodeId.getId());
				PCase caze = getCase(node.getCaseId());
				if (isProcessActive(caze)) {
					doneCnt++;
					FlowNodeExecutor executor = new FlowNodeExecutor(node);
					Thread thread = new Thread(executor);
					executor.thread = thread;
					thread.start();
					if (threads.size() >= maxThreads) break;
				}
			}
			
			while (threads.size() > 0) {
				threads.removeIf(e -> e.isFinished());
				MThread.sleep(200);
			}
		} else {
			for (PNodeInfo nodeId : result) {
				doneCnt++;
				synchronized (running) {
					if (running.contains(nodeId)) continue;
				}
				PNode node = getFlowNode(nodeId.getId());
				PCase caze = getCase(node.getCaseId());
				if (isProcessActive(caze)) {
					synchronized (running) {
						running.add(nodeId.getId());
					}
					doFlowNode(node);
					synchronized (running) {
						running.remove(nodeId);
					}
				}
			}
		}
		result.close();
				
		config.listener.doStep("execute finished");
	
		return doneCnt;
	}
	
	public boolean isProcessActive(PCase caze) {
		try {
			MUri uri = MUri.toUri(caze.getUri());
			EProcess process = getProcess(uri);
			if (process == null) return false;
			EPool pool = getPool(process, uri);
			if (pool == null) return false;
			return pool.getPoolClass() != null;
		} catch (Throwable t) {}
		return false;
	}

	public void cleanup() throws IOException, NotFoundException {
				
		// scan for closeable cases and runtimes
		config.listener.doStep("cleanup");
		for (PCaseInfo caseId : storage.getCases(STATE_CASE.RUNNING)) {
			PCase caze = getCase(caseId.getId());
			boolean found = false;
			HashSet<UUID> allRuntime = new HashSet<>();
			HashSet<UUID> usedRuntime = new HashSet<>();
			
			for ( PNodeInfo nodeId : storage.getFlowNodes(caze.getId(), null)) {
				PNode node = getFlowNode(nodeId.getId());
				if (node.getType() == TYPE_NODE.RUNTIME && node.getState() == STATE_NODE.WAITING) {
					allRuntime.add(node.getId());
				} else
				if (node.getType() != TYPE_NODE.RUNTIME && node.getState() != STATE_NODE.CLOSED) {
					found = true;
					usedRuntime.add(node.getRuntimeId());
				}
			}
			
			// close unused runtimes
			allRuntime.removeIf(u -> usedRuntime.contains(u));
			for (UUID rId : allRuntime) {
				try {
					PNode runtime = getFlowNode(rId);
					closeRuntime(runtime);
				} catch (Throwable t) {
					log().e(rId,t);
					config.listener.error(rId,t);
				}
			}
			if (!found) {
				// close case without active node
				closeCase(caze, false);
			}
			
		}
		
		config.listener.doStep("cleanup finished");
				
	}

	private class FlowNodeExecutor implements Runnable {

		public boolean finished = false;
		public boolean outtimed = false;
		@SuppressWarnings("unused")
		public Thread thread;
		private PNode node;
		long start = System.currentTimeMillis();

		public FlowNodeExecutor(PNode node) {
			this.node = node;
		}

		public boolean isFinished() {
			if (finished || outtimed) return true;
			try {
				if (MTimeInterval.isTimeOut(start, node.getActivityTimeout())) {
					config.listener.error("activity timeout",node);
					outtimed = true;
					return true;
				}
			} catch (Throwable t) {
				t.printStackTrace(); // should not happen
			}
			return false;
		}

		@Override
		public void run() {
			synchronized (running) {
				running.add(node.getId());
			}
			start = System.currentTimeMillis();
			try {
				doFlowNode(node);
			} catch (Throwable t) {
				try {
					log().e(node,t);
					config.listener.error(node,t);
				} catch (Throwable t2) {}
			}
			synchronized (running) {
				running.remove(node.getId());
			}
			finished = true;
		}
		
	}

	public List<UUID> getRunning() {
		synchronized (running) {
			return new LinkedList<UUID>(running);
		}
	}
	
	private void saveFlowNode(EngineContext context, PNode flow, AActivity<?> activity) throws IOException {
		config.listener.saveFlowNode(flow,activity);
		if (activity != null) {
			try {
				Map<String, Object> newParameters = activity.exportParamters();
				flow.getParameters().putAll(newParameters);
			} catch (Throwable t) {
				log().e(t);
				config.listener.error(flow,activity,t);
				// set failed
				flow.setSuspendedState(flow.getState());
				flow.setState(STATE_NODE.STOPPED);
			}
		}
		synchronized (nodeCache) {
			storage.saveFlowNode(flow);
			nodeCache.put(flow.getId(), flow);
			flow.updateStartState();
		}
		if (context != null)
			savePCase(context);
	}

	public void closeCase(UUID caseId, boolean hard) throws IOException, NotFoundException {
		PCase caze = getCase(caseId);
		closeCase(caze, hard);
	}
	
	public void closeCase(PCase caze, boolean hard) throws IOException {
		config.listener.closeCase(caze,hard);
		if (!hard) {
			try {
				MUri uri = MUri.toUri(caze.getUri());
				EProcess process = getProcess(uri);
				EPool pool = getPool(process, uri);
				
				EngineContext context = new EngineContext(this);
				context.setUri(uri.toString());
				context.setEProcess(process);
				context.setEPool(pool);
				context.setPCase(caze);
		
				APool<?> aPool = context.getPool();
				aPool.closeCase();
				Map<String, Object> newParameters = aPool.exportParamters();
				caze.getParameters().putAll(newParameters);
			} catch (Throwable t) {
				log().e(caze,t);
				config.listener.error(caze,t);
			}
		}
		caze.setState(STATE_CASE.CLOSED);
		synchronized (caseCache) {
			storage.saveCase(caze);
			caseCache.put(caze.getId(), caze);
		}
	}

	public void closeRuntime(PNode pNode) throws MException, IOException {
		config.listener.closeRuntime(pNode);
		PCase caze = null;
		try {
			caze = getCase(pNode.getCaseId());
		} catch (Throwable t) {
			log().e(pNode.getCaseId(),t);
			config.listener.error(pNode,t);
			return; // ignore - try next time
		}
			
		if (caze.getState() != STATE_CASE.RUNNING) {
			pNode.setScheduled(newScheduledTime(pNode));
			return;
		}
		
		// create context
		EngineContext context = createContext(caze, pNode);

		RuntimeNode aNode = createRuntimeObject(context, pNode);
		aNode.close();
		
		pNode.setState(STATE_NODE.CLOSED);
		saveRuntime(pNode, aNode);

	}

	private void doFlowNode(PNode pNode) {
		config.listener.doFlowNode(pNode);
		try {
			
			PCase caze = null;
			try {
				caze = getCase(pNode.getCaseId());
			} catch (Throwable t) {
				log().e(pNode.getCaseId(),t);
				config.listener.error(pNode,t);
				return; // ignore - try next time
			}
			
			if (caze == null) {
				// node without case ... puh
				config.listener.error("node without case",pNode,pNode.getCaseId());
				closeFlowNode(null, pNode, STATE_NODE.STOPPED);
				return;
			}
				
			if (caze.getState() != STATE_CASE.RUNNING) {
				pNode.setScheduled(newScheduledTime(pNode));
				config.listener.doFlowNodeScheduled(pNode);
				return;
			}
			
			// create context
			EngineContext context = createContext(caze, pNode);

			// check for timer trigger
			Entry<String, Long> nextScheduled = pNode.getNextScheduled();
			if (!nextScheduled.getKey().equals("")) {
				// do trigger
				Trigger trigger = getTrigger(context,nextScheduled.getKey());
				if (trigger == null) {
					// set to error
					log().e("Unknown trigger",pNode,nextScheduled.getKey());
					context.getARuntime().doErrorMsg(pNode, "Unknown trigger",nextScheduled.getKey());
					config.listener.error("Unknown trigger",pNode,nextScheduled.getKey());
					closeFlowNode(context, pNode, STATE_NODE.STOPPED);
					return;
				}
				Class<? extends AActivity<?>> next = trigger.activity();
				EElement eNext = context.getEPool().getElement(next.getCanonicalName());
				createActivity(context, pNode, eNext);
				// close this
				closeFlowNode(context, pNode, STATE_NODE.CLOSED);
				return;
			}
			
			// do lifecycle
			doNodeLifecycle(context, pNode);
			
		} catch (Throwable t) {
			log().e(pNode,t);
			config.listener.error(pNode,t);
		}
	}

	public Trigger getTrigger(EngineContext context, String key) {
		
		if (key.startsWith("[")) {
			int index = MCast.toint(key.substring(1), -1);
			Trigger[] list = context.getENode().getTriggers();
			if (index < 0 || list.length <= index ) return null;
			return list[index];
		}
		for (Trigger trigger : context.getENode().getTriggers()) {
			if (trigger.name().equals(key))
				return trigger;
		}
		return null;
	}

	private void doNodeErrorHandling(EngineContext context, PNode pNode, Throwable t) {
		config.listener.doNodeErrorHandling(context,pNode,t);
		if (!(t instanceof TechnicalException)) {
			EElement eNode = context.getENode();
			Trigger defaultError = null;
			Trigger errorHandler = null;
			for (Trigger trigger : eNode.getTriggers()) {
				if (trigger.type() == TYPE.DEFAULT_ERROR)
					defaultError = trigger;
				else
				if (trigger.type() == TYPE.ERROR) {
					if (t instanceof TaskException){
						if (trigger.name().equals(((TaskException)t).getTrigger()))
							errorHandler = trigger;
					}
				}
			}
			if (errorHandler == null) errorHandler = defaultError;
			if (errorHandler != null) {
				// create new activity
				EElement start = context.getEPool().getElement(errorHandler.activity().getCanonicalName());
				try {
					createActivity(context, pNode, start);
					// close node
					closeFlowNode(context,pNode,STATE_NODE.CLOSED);
					return;
				} catch (Exception e) {
					log().e(e);
					config.listener.error(pNode,start,e);
				}
			}
		}
		
		try {
			closeFlowNode(context, pNode, STATE_NODE.FAILED);
		} catch (IOException e) {
			log().e(pNode,e);
			config.listener.error(pNode,e);
		}
	}

	public void closeFlowNode(EngineContext context, PNode pNode, STATE_NODE state) throws IOException {
		config.listener.closeFlowNode(pNode, state);
		if (context != null)
			context.getARuntime().closedActivity(pNode);
		pNode.setState(state);
		synchronized (nodeCache) {
			storage.saveFlowNode(pNode);
			nodeCache.put(pNode.getId(), pNode);
		}
		if (context != null)
			savePCase(context);
	}

	public PCase getCase(UUID caseId) throws NotFoundException, IOException {
		synchronized (caseCache) {
			PCase caze = caseCache.get(caseId);
			if (caze == null) {
				caze = storage.loadCase(caseId);
				caseCache.put(caseId, caze);
			}
			return caze;
		}
	}

	public PNode getFlowNode(UUID nodeId) throws NotFoundException, IOException {
		synchronized (nodeCache) {
			PNode node = nodeCache.get(nodeId);
			if (node == null) {
				node = storage.loadFlowNode(nodeId);
				nodeCache.put(nodeId, node);
			}
			return node;
		}
	}

// ---
		
	public UUID start(String uri) throws Exception {
		MUri u = MUri.toUri(uri);
		Map<String, String> q = u.getQuery();
		MProperties properties = new MProperties();
		if (q != null)
			properties.putAll(q);
		return start(u, properties);
	}
	
	public UUID start(String uri, IProperties properties) throws Exception {
		MUri u = MUri.toUri(uri);
		return start(u, properties);
	}
	
	public UUID start(MUri uri, IProperties properties) throws Exception {
		if (!EngineConst.SCHEME_REACTIVE.equals(uri.getScheme()))
			throw new UsageException("unknown uri scheme",uri,"should be",EngineConst.SCHEME_REACTIVE);
		
		if (properties == null) {
			properties = new MProperties();
		}
		Map<String, String> query = uri.getQuery();
		if (query != null)
			properties.putAll(query);

		// get process
		EProcess process = getProcess(uri);
		
		if (process == null) throw new NotFoundException("process unknown",uri);
				
		// load pool
		EPool pool = getPool(process, uri);
		
		if (pool == null)
			throw new NotFoundException("pool not found in process",uri);
		
		// update uri
		MutableUri u = new MutableUri(uri.toString());
		u.setLocation(process.getProcessName() + ":" + process.getVersion() );
		u.setPath(pool.getCanonicalName());
		u.setQuery(null);
		MUri originalUri = uri;
		uri = u;
		
		// load start points
		List<EElement> startPoints = null;
		String fragment = uri.getFragment();
		if (fragment != null) {
			EElement point = pool.getElement(fragment);
			if (point == null)
				throw new MException("start point not found",fragment,uri);
			if (!point.is(AStartPoint.class))
				throw new MException("node is not a start point",uri);
			startPoints = new LinkedList<>();
			startPoints.add(point);
		} else {
			startPoints = pool.getStartPoints();
			// remove inactive startpoints from list
			startPoints.removeIf(s -> s.isInterface(InactiveStartPoint.class));
		}
		
		if (startPoints.size() == 0)
			throw new NotFoundException("no start point found",uri);
		
		String[] uriParams = uri.getParams();
		MProperties options = null;
		if (uriParams != null && uriParams.length > 0) {
			options = MProperties.explodeToMProperties(uriParams);
		} else {
			options = new MProperties();
		}
		
		String createdBy = config.aaa.getCurrentUserId();
		
		// everything fine ... start creating
		config.listener.startCase(originalUri,uri, properties, process, pool, startPoints, options, createdBy);

		// the context
		EngineContext context = new EngineContext(this);
		context.setUri(uri.toString());
		context.setEProcess(process);
		context.setEPool(pool);
		
		// create the PCase
		PCase pCase = new PCase(
				UUID.randomUUID(), 
				options, 
				uri.toString(), 
				pool.getName(),
				pool.getCanonicalName(), 
				System.currentTimeMillis(),
				createdBy,
				STATE_CASE.NEW, 
				0,
				properties
			);
		context.setPCase(pCase);
		
		// create the APool
		APool<?> aPool = createPoolObject(pool);
		context.setAPool(aPool);
		
		// life cycle case pool
		if (aPool instanceof ContextRecipient)
			((ContextRecipient)aPool).setContext(context);
		aPool.initializeCase(properties);
		pCase.getParameters().clear(); // cleanup before first save, will remove parameters from external input
		savePCase(pCase, aPool);
		
		// create start point flow nodes
		Throwable isError = null;
		for (EElement start : startPoints) {
			try {
				createStartPoint(context, start);
			} catch (Throwable t) {
				log().w(start,t);
				config.listener.error(pCase,start,t);
				isError = t;
				break;
			}
		}
		if (isError != null) {
			storage.deleteCaseAndFlowNodes(pCase.getId());
			throw new Exception(isError);
		}
		
		pCase.setState(STATE_CASE.RUNNING);
		savePCase(pCase,aPool);
		
		return pCase.getId();
	}

	public void savePCase(EngineContext context) throws IOException {
		savePCase(context.getPCase(), context.getPool());
	}
	
	public void savePCase(PCase pCase, APool<?> aPool) throws IOException {
		Map<String, Object> newParameters = aPool.exportParamters();
		pCase.getParameters().putAll(newParameters);
		config.listener.saveCase(pCase, aPool);
		synchronized (caseCache) {
			caseCache.put(pCase.getId(), pCase);
			storage.saveCase(pCase);
		}
	}

	public EPool getPool(EProcess process, MUri uri) throws NotFoundException {
		String poolName = uri.getPath();
		if (MString.isEmpty(poolName))
			poolName = process.getProcessDescription().defaultPool();
		if (MString.isEmpty(poolName))
			throw new NotFoundException("default pool not found for process",uri);
		
		EPool pool = process.getPool(poolName);
		return pool;
	}

	public EProcess getProcess(MUri uri) throws MException {
		// load process
		String processName = uri.getLocation();
		String processVersion = null;
		if (MString.isIndex(processName, ':')) {
			processVersion = MString.afterIndex(processName, ':');
			processName = MString.beforeIndex(processName, ':');
		}
		if (processVersion == null)
			processVersion = (String) config.persistent.getParameters().get("process:" + processName + ":enabled");
		else {
			String[] versions = ((String) config.persistent.getParameters().getOrDefault("process:" + processName + ":versions","")).split(",");
			if (!MCollection.contains(versions, processVersion))
				throw new MException("specified process version is not enabled",processName, processVersion,versions);
		}
		if (MString.isEmpty(processVersion))
			throw new MException("default process version is disabled",processName,uri);
		
		EProcess process = processProvider.getProcess(processName, processVersion);
		return process;
	}

	public void createStartPoint(EngineContext context, EElement start) throws Exception {
		
		// some checks
		if (!start.is(AStartPoint.class))
			throw new MException("activity is not a start point",context,start);
		
		if (!context.getEPool().isElementOfPool(start))
			throw new MException("start point is not part of the pool",context,start);

		// collect information
		ActivityDescription desc = start.getActivityDescription();
		Class<? extends RuntimeNode> runtimeClass = desc.runtime();
		
		UUID caseId = context.getPCase().getId();
		
		config.listener.createStartPoint(context.getPCase(),start);
		
		// create runtime
		PNode runtime = new PNode(
				UUID.randomUUID(), 
				caseId,
				"runtime",
				runtimeClass.getCanonicalName(),
				System.currentTimeMillis(),
				0,
				STATE_NODE.WAITING,
				STATE_NODE.WAITING,
				null,
				null,
				null,
				false,
				TYPE_NODE.RUNTIME,
				null,
				new HashMap<>(),
				null,
				null,
				0
				);
		config.listener.createRuntime(context.getPCase(),start,runtime);
		synchronized (nodeCache) {
			storage.saveFlowNode(runtime);
			nodeCache.put(runtime.getId(), runtime);
		}
		context.setPRuntime(runtime);
		UUID runtimeId = runtime.getId();
		
		// create flow node
		PNode flow = new PNode(
				UUID.randomUUID(), 
				caseId, 
				start.getName(),
				start.getCanonicalName(),
				System.currentTimeMillis(),
				0,
				STATE_NODE.NEW,
				STATE_NODE.NEW, 
				null,
				null,
				null,
				false, 
				TYPE_NODE.NODE, 
				null, 
				null, 
				null, 
				runtimeId,
				EngineConst.TRY_COUNT
			);
		flow.setScheduled(System.currentTimeMillis());
		config.listener.createStartNode(context.getPCase(),start,flow);
		context = new EngineContext(context, flow);
		
		doNodeLifecycle(context, flow);
	}
	
	public PNode createActivity(EngineContext context, PNode previous, EElement start) throws Exception {
		
		UUID caseId = context.getPCase().getId();
		UUID runtimeId = previous.getRuntimeId();
		UUID previousId = previous.getId();
		
		// create flow node
		PNode flow = new PNode(
				UUID.randomUUID(), 
				caseId, 
				start.getName(),
				start.getCanonicalName(),
				System.currentTimeMillis(),
				0,
				STATE_NODE.NEW,
				STATE_NODE.NEW, 
				null,
				null,
				null,
				false, 
				TYPE_NODE.NODE, 
				null, 
				null, 
				null, 
				runtimeId,
				EngineConst.TRY_COUNT
			);
		flow.setScheduled(System.currentTimeMillis());
		config.listener.createActivity(context.getPCase(),previous,start,flow);
		context.getARuntime().createActivity(flow, previousId);
		context = new EngineContext(context, flow);
		
		doNodeLifecycle(context, flow);
		
		return flow;
	}
	
	protected void doNodeLifecycle(EngineContext context, PNode flow) throws Exception {
		
		boolean init = flow.getStartState() == STATE_NODE.NEW; // this means the node is not executed!
		context = new EngineContext(context, flow);
		
		// lifecycle flow node
		EElement start = context.getENode();
		AActivity<?> activity = (AActivity<?>) createActivityObject(start);
		context.setANode(activity);
		RuntimeNode runtime = context.getARuntime();
		config.listener.doNodeLifecycle(start,activity,runtime,flow,init);
		runtime.doNodeLifecycle(flow,init);
		if (activity instanceof ContextRecipient)
			((ContextRecipient)activity).setContext(context);
		try {
			if (init) {
				activity.initializeActivity();
			} else {
				flow.setLastRunDate(System.currentTimeMillis());
				activity.doExecuteActivity();
			}
			// secure switch state away from NEW
			if (flow.getState() == STATE_NODE.NEW)
				flow.setState(STATE_NODE.RUNNING);
			else
			if (flow.getStartState() == STATE_NODE.RUNNING && flow.getState() == STATE_NODE.RUNNING) {
				flow.setTryCount(flow.getTryCount()-1);
				if (flow.getTryCount() <= 0) {
					flow.setState(STATE_NODE.STOPPED);
				} else {
					flow.setScheduled(newScheduledTime(flow));
				}
			}
		} catch (Throwable t) {
			// remember
			config.listener.error(flow,t);
			runtime.doActivityFailed(flow);
			doNodeErrorHandling(context, flow, t);
			return;
		}
		if (init)
			flow.getParameters().clear();
		
		// save
		saveFlowNode(context, flow,activity);
		
		runtime.doActivityStop(flow);
	}
	
	private long newScheduledTime(PNode flow) {
		return System.currentTimeMillis() + MTimeInterval.MINUTE_IN_MILLISECOUNDS;
	}

	public APool<?> createPoolObject(EPool pool) throws InstantiationException, IllegalAccessException {
		return pool.getPoolClass().newInstance();		
	}

	public AElement<?> createActivityObject(EElement element) throws InstantiationException, IllegalAccessException {
		return element.getElementClass().newInstance();		
	}

	/**
	 * Create a runtime node from P-Object.
	 * 
	 * @param context
	 * @param runtime
	 * @return Runtime object
	 */
	public RuntimeNode createRuntimeObject(EngineContext context, PNode runtime) {
		String canonicalName = runtime.getCanonicalName();
		RuntimeNode out = null;
		if (RuntimeNode.class.getCanonicalName().equals(canonicalName))
			out = new RuntimeNode();
		else
			out = (RuntimeNode)context.getEPool().getElement(canonicalName);
		// lifecycle
		if (out instanceof ContextRecipient)
			((ContextRecipient)out).setContext(context);
		
		out.importParameters(runtime.getParameters());
		
		return out;
	}

	/**
	 * Load the runtime P-Object from storage.
	 * 
	 * @param context
	 * @param pNode
	 * @return The runtime persistent object
	 */
	public PNode getRuntimeForPNode(EngineContext context, PNode pNode) {
		if (pNode == null || pNode.getRuntimeId() == null) return null;
		try {
			return getFlowNode(pNode.getRuntimeId());
		} catch (NotFoundException | IOException e) {
			log().w(pNode,e);
			return null;
		}
	}

	/**
	 * Return the object of the swim lane.
	 * 
	 * @param context
	 * @param eNode
	 * @return The Swim lane object
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public ASwimlane<?> createSwimlaneObject(EngineContext context, EElement eNode) throws InstantiationException, IllegalAccessException {
		ASwimlane<?> out = eNode.getSwimlane().newInstance();
		// lifecycle
		if (out instanceof ContextRecipient)
			((ContextRecipient)out).setContext(context);
		return out;
	}

	/**
	 * Save the runtime to storage. If runtme object is given, the
	 * parameters will be stored.
	 * 
	 * @param pRuntime
	 * @param aRuntime
	 * @throws IOException
	 */
	public void saveRuntime(PNode pRuntime, RuntimeNode aRuntime) throws IOException {
		if (aRuntime != null) {
			Map<String, Object> parameters = aRuntime.exportParamters();
			if (parameters != null) {
				pRuntime.getParameters().putAll(parameters);
			}
		}
		config.listener.saveRuntime(pRuntime, aRuntime);
		synchronized (nodeCache) {
			storage.saveFlowNode(pRuntime);
			nodeCache.put(pRuntime.getId(), pRuntime);
		}
	}

	/**
	 * Archive all closed cases.
	 * 
	 */
	public void archiveAll() {
		try {
			archive.saveEngine(config.persistent);
			for (PCaseInfo caseId : storage.getCases(STATE_CASE.CLOSED)) {
				PCase caze = getCase(caseId.getId());
				config.listener.archiveCase(caze);
				archive.saveCase(caze);
				for (PNodeInfo nodeId : storage.getFlowNodes(caze.getId(), null)) {
					PNode node = getFlowNode(nodeId.getId());
					archive.saveFlowNode(node);
				}
				storage.deleteCaseAndFlowNodes(caze.getId());
			}
		} catch (Throwable t) {
			log().e(t);
		}
	}

	/**
	 * This will archive the case and delete it in from storage.
	 * 
	 * @param caseId
	 * @throws IOException
	 * @throws MException 
	 */
	public void archiveCase(UUID caseId) throws IOException, MException {
		try {
			PCase caze = getCase(caseId);
			if (caze != null) {
				config.listener.archiveCase(caze);
				if (caze.getState() != STATE_CASE.CLOSED)
					throw new MException("case is not closed",caseId);
				archive.saveCase(caze);
			}
		} catch (NotFoundException e) {
			log().d(caseId,e);
		}
		for (PNodeInfo nodeId : storage.getFlowNodes(caseId, null)) {
			PNode node = getFlowNode(nodeId.getId());
			archive.saveFlowNode(node);
		}
		storage.deleteCaseAndFlowNodes(caseId);
	}

	/**
	 * Set a case and all nodes to suspended state.
	 * 
	 * @param caseId
	 * @throws IOException
	 * @throws MException
	 */
	public void suspendCase(UUID caseId) throws IOException, MException {
		PCase caze = getCase(caseId);
		if (caze.getState() == STATE_CASE.SUSPENDED)
			throw new MException("case already suspended",caseId);
		config.listener.suspendCase(caze);
		caze.setState(STATE_CASE.SUSPENDED);
		storage.saveCase(caze);
		
		for (PNodeInfo nodeId : storage.getFlowNodes(caseId, null)) {
			PNode node = getFlowNode(nodeId.getId());
			if (node.getState() != STATE_NODE.SUSPENDED && node.getState() != STATE_NODE.CLOSED) {
				node.setSuspendedState(node.getState());
				node.setState(STATE_NODE.SUSPENDED);
				storage.saveFlowNode(node);
			}
		}
	}

	/**
	 * Return a suspended case and all nodes to 'normal' state.
	 * 
	 * @param caseId
	 * @throws IOException
	 * @throws MException
	 */
	public void resumeCase(UUID caseId) throws IOException, MException {
		PCase caze = getCase(caseId);
		if (caze.getState() != STATE_CASE.SUSPENDED)
			throw new MException("already is not suspended",caseId);
		config.listener.unsuspendCase(caze);
		caze.setState(STATE_CASE.RUNNING);
		storage.saveCase(caze);
		
		for (PNodeInfo nodeId : storage.getFlowNodes(caseId, null)) {
			PNode node = getFlowNode(nodeId.getId());
			if (node.getState() == STATE_NODE.SUSPENDED && node.getSuspendedState() != STATE_NODE.NEW) {
				node.setState(node.getSuspendedState());
				node.setSuspendedState(STATE_NODE.NEW);
				storage.saveFlowNode(node);
			}
		}
	}
	
	/**
	 * Set flow node to stopped. Not possible for suspended nodes.
	 * 
	 * @param nodeId
	 * @throws MException
	 * @throws IOException
	 */
	public void cancelFlowNode(UUID nodeId) throws MException, IOException {
		PNode node = getFlowNode(nodeId);
		if (node.getStartState() == STATE_NODE.SUSPENDED)
			throw new MException("node is suspended",nodeId);
		config.listener.cancelFlowNode(node);
		node.setSuspendedState(node.getState());
		node.setState(STATE_NODE.CLOSED);
		storage.saveFlowNode(node);
	}
	
	/**
	 * Set a flow node to running state. Not possible for suspended nodes.
	 * @param nodeId
	 * @throws IOException
	 * @throws MException
	 */
	public void retryFlowNode(UUID nodeId) throws IOException, MException {
		PNode node = getFlowNode(nodeId);
		if (node.getStartState() == STATE_NODE.SUSPENDED)
			throw new MException("node is suspended",nodeId);
		config.listener.retryFlowNode(node);
		node.setSuspendedState(node.getState());
		node.setState(STATE_NODE.RUNNING);
		node.setScheduled(System.currentTimeMillis());
		storage.saveFlowNode(node);
	}
	
	/**
	 * Migrate the content of a running case. The case must be suspended or closed.
	 * The case will be archived before migration. It is possible to restore the case
	 * after fail.
	 * 
	 * @param caseId
	 * @param migrator
	 * @throws MException
	 * @throws IOException
	 */
	public void migrateCase(UUID caseId, Migrator migrator) throws MException, IOException {
		PCase caze = getCase(caseId);
		if (caze.getState() != STATE_CASE.SUSPENDED && caze.getState() != STATE_CASE.CLOSED)
			throw new MException("already is not suspended",caseId);
		
		config.listener.migrateCase(caze,migrator);
		
		// create context
		EngineContext context = createContext(caze);
		
		LinkedList<PNode> nodes = new LinkedList<>();
		for (PNodeInfo nodeId : storage.getFlowNodes(caseId, null)) {
			PNode node = getFlowNode(nodeId.getId());
			nodes.add(node);
		}
		
		// archive the case state
		archive.saveCase(caze);
		for (PNode node : nodes)
			archive.saveFlowNode(node);

		// migrate
		migrator.doMigrate(context, nodes);
		
		// validate output
		if (context.getPCase().getState() != STATE_CASE.SUSPENDED && context.getPCase().getState() != STATE_CASE.CLOSED)
			throw new MException("It's not allowed to change case state, change suspendedState instead");
		for (PNode node : nodes)
			if (node.getState() != STATE_NODE.SUSPENDED && node.getState() != STATE_NODE.CLOSED)
				throw new MException("It's not allowed to change flow node state, change suspendedState instead");
		
		// store
		storage.saveCase(context.getPCase());
		for (PNode node : nodes)
			storage.saveFlowNode(node);
		
	}
	
	/**
	 * Copy the case from the archive to the storage. Only suspended, closed
	 * or deleted cases can be restored. The restored case will be in suspended state.
	 * 
	 * @param caseId
	 * @throws IOException
	 * @throws MException
	 */
	public void restoreCase(UUID caseId) throws IOException, MException {
		try {
			PCase caze = getCase(caseId);
			if (caze != null) {
				config.listener.archiveCase(caze);
				if (caze.getState() != STATE_CASE.CLOSED && caze.getState() != STATE_CASE.SUSPENDED)
					throw new MException("case is not closed or suspended",caseId);
			}
		} catch (NotFoundException e) {
			log().d(caseId,e);
		}
		PCase caze = archive.loadCase(caseId);
		config.listener.restoreCase(caze);
		caze.setState(STATE_CASE.SUSPENDED);
		storage.saveCase(caze);
		for (PNodeInfo nodeId : archive.getFlowNodes(caseId, null)) {
			PNode node = getFlowNode(nodeId.getId());
			// set to suspended
			if (node.getState() != STATE_NODE.CLOSED && node.getState() != STATE_NODE.SUSPENDED) {
				node.setSuspendedState(node.getState());
				node.setState(STATE_NODE.SUSPENDED);
			}
			storage.saveFlowNode(node);
		}
		
	}
	
	// -- aaa provider
	
	public AaaProvider getAaaProvider() {
		return config.aaa;
	}

	// -- storage
	
	public Result<PCaseInfo> storageGetCases(STATE_CASE state) throws IOException {
		return storage.getCases(state);
	}

	public Result<PNodeInfo> storageGetFlowNodes(UUID caseId, STATE_NODE state) throws IOException {
		return storage.getFlowNodes(caseId, state);
	}

	public Result<PNodeInfo> storageGetScheduledFlowNodes(STATE_NODE state, long scheduled) throws IOException {
		return storage.getScheduledFlowNodes(state, scheduled);
	}

	public Result<PNodeInfo> storageGetSignaledFlowNodes(STATE_NODE state, String signal) throws IOException {
		return storage.getSignaledFlowNodes(state, signal);
	}

	public Result<PNodeInfo> storageGetMessageFlowNodes(UUID caseId, STATE_NODE state, String message) throws IOException {
		return storage.getMessageFlowNodes(caseId, state, message);
	}

	// -- archive

	public PCase archiveLoadCase(UUID id) throws IOException, NotFoundException {
		return archive.loadCase(id);
	}

	public PNode archiveLoadFlowNode(UUID id) throws IOException, NotFoundException {
		return archive.loadFlowNode(id);
	}

	public Result<PCaseInfo> archiveGetCases(STATE_CASE state) throws IOException {
		return archive.getCases(state);
	}

	public Result<PNodeInfo> archiveGetFlowNodes(UUID caseId, STATE_NODE state) throws IOException {
		return archive.getFlowNodes(caseId, state);
	}

	public Result<PNodeInfo> archiveGetScheduledFlowNodes(STATE_NODE state, long scheduled) throws IOException {
		return archive.getScheduledFlowNodes(state, scheduled);
	}

	public Result<PNodeInfo> archiveGetSignaledFlowNodes(STATE_NODE state, String signal) throws IOException {
		return archive.getSignaledFlowNodes(state, signal);
	}

	public Result<PNodeInfo> archiveGetMessageFlowNodes(UUID caseId, STATE_NODE state, String message) throws IOException {
		return archive.getMessageFlowNodes(caseId, state, message);
	}

	// --
	
	public EngineContext createContext(PCase caze) throws MException {
		
		MUri uri = MUri.toUri(caze.getUri());
		EProcess process = getProcess(uri);
		EPool pool = getPool(process, uri);
		
		EngineContext context = new EngineContext(this);
		context.setUri(uri.toString());
		context.setEProcess(process);
		context.setEPool(pool);
		context.setPCase(caze);
		return context;

	}

	public EngineContext createContext(PCase caze, PNode pNode) throws MException {
		
		MUri uri = MUri.toUri(caze.getUri());
		EProcess process = getProcess(uri);
		EPool pool = getPool(process, uri);
		
		EngineContext context = new EngineContext(this, pNode);
		context.setUri(uri.toString());
		context.setEProcess(process);
		context.setEPool(pool);
		context.setPCase(caze);
		return context;
	}

	public void doCloseActivity(RuntimeNode runtimeNode, UUID nodeId) throws IOException, MException {
		PNode node = getFlowNode(nodeId);
		PCase caze = getCase(node.getCaseId());
		EngineContext context = createContext(caze, node);
		AElement<?> aNode = context.getANode();
		((CloseActivity)aNode).doClose(context,runtimeNode);
		savePCase(context);
	}

	public void saveEnginePersistence() {
		try {
			storage.saveEngine(config.persistent);
		} catch (IOException e) {
			log().e(e);
		}
	}

	public void loadEnginePersistence() {
		try {
			config.persistent = storage.loadEngine();
		} catch (NotFoundException | IOException e) {
			log().e(e);
		}
	}

}
