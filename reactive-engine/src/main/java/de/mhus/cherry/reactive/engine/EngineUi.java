package de.mhus.cherry.reactive.engine;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import de.mhus.cherry.reactive.model.activity.Actor;
import de.mhus.cherry.reactive.model.engine.ContextRecipient;
import de.mhus.cherry.reactive.model.engine.EPool;
import de.mhus.cherry.reactive.model.engine.EProcess;
import de.mhus.cherry.reactive.model.engine.PCase;
import de.mhus.cherry.reactive.model.engine.PCase.STATE_CASE;
import de.mhus.cherry.reactive.model.engine.PCaseInfo;
import de.mhus.cherry.reactive.model.engine.PNode;
import de.mhus.cherry.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.cherry.reactive.model.engine.PNodeInfo;
import de.mhus.cherry.reactive.model.ui.ICase;
import de.mhus.cherry.reactive.model.ui.INode;
import de.mhus.lib.core.MLog;
import de.mhus.lib.core.matcher.Context;
import de.mhus.lib.core.util.MUri;
import de.mhus.lib.core.util.SoftHashMap;
import de.mhus.lib.errors.NotFoundException;

public class EngineUi extends MLog {

	private Engine engine;
	private String user;
	private SoftHashMap<String, Boolean> cacheAccess = new SoftHashMap<>();
	private SoftHashMap<String, EngineContext> cacheContext = new SoftHashMap<>();

	public EngineUi(Engine engine, String user) {
		this.engine = engine;
		this.user = user;
	}
	
	public List<ICase> getCases(int page, int size, STATE_CASE state) throws IOException, NotFoundException {
		LinkedList<ICase> out = new LinkedList<>();
		int cnt = 0;
		int first = page * size;
		for (PCaseInfo info : engine.storageGetCases(state)) {
			if (hasReadAccess(info.getUri())) {
				if (cnt >= first) {
					PCase caze = engine.getCase(info.getId());
					out.add(new ICase(getContext(caze.getUri()), caze));
				}
				cnt++;
				if (out.size() >= size) break;
			}
		}
		return out;
	}
	
	public List<INode> getNodes(int page, int size, STATE_NODE state) throws NotFoundException, IOException {
		LinkedList<INode> out = new LinkedList<>();
		int cnt = 0;
		int first = page * size;
		for (PNodeInfo info : engine.storageGetFlowNodes(null,state)) {
			PCase caze = engine.getCase(info.getCaseId());
			if (hasReadAccess(caze.getUri())) {
				try {
					if (cnt >= first) {
						PNode node = engine.getFlowNode(info.getId());
						out.add(new INode(getContext(caze.getUri()), caze, node));
					}
					cnt++;
				} catch (Exception e) {
					log().d(info,e);
				}
				if (out.size() >= size) break;
			}
		}
		return out;
	}

	public List<INode> getAssignedNodes(int page, int size) throws NotFoundException, IOException {
		LinkedList<INode> out = new LinkedList<>();
		int cnt = 0;
		int first = page * size;
		for (PNodeInfo info : engine.storageGetAssignedFlowNodes(user)) {
			PCase caze = engine.getCase(info.getCaseId());
			try {
				if (cnt >= first) {
					PNode node = engine.getFlowNode(info.getId());
					out.add(new INode(getContext(caze.getUri()), caze, node));
				}
				cnt++;
			} catch (Exception e) {
				log().d(info,e);
			}
			if (out.size() >= size) break;
		}
		return out;
	}

	public List<INode> getUnassignedNodes(int page, int size) throws NotFoundException, IOException {
		LinkedList<INode> out = new LinkedList<>();
		int cnt = 0;
		int first = page * size;
		for (PNodeInfo info : engine.storageGetAssignedFlowNodes(null)) {
			PCase caze = engine.getCase(info.getCaseId());
			if (hasReadAccess(caze.getUri())) {
				try {
					if (cnt >= first) {
						PNode node = engine.getFlowNode(info.getId());
						out.add(new INode(getContext(caze.getUri()),caze, node));
					}
					cnt++;
				} catch (Exception e) {
					log().d(info,e);
				}
				if (out.size() >= size) break;
			}
		}
		return out;
	}
	
	public List<INode> getNodes(int page, int size, UUID caseId, STATE_NODE state) throws NotFoundException, IOException {
		LinkedList<INode> out = new LinkedList<>();
		int cnt = 0;
		int first = page * size;
		PCase caze = engine.getCase(caseId);
		if (hasReadAccess(caze.getUri())) {
			for (PNodeInfo info : engine.storageGetFlowNodes(caseId,state)) {
					try {
						if (cnt >= first) {
							PNode node = engine.getFlowNode(info.getId());
							out.add(new INode(getContext(caze.getUri()), caze, node));
						}
						cnt++;
					} catch (Exception e) {
						log().d(info,e);
					}
					if (out.size() >= size) break;
			}
		}
		return out;
	}
	
	private EngineContext getContext(String uri) {
		synchronized (cacheContext) {
			EngineContext context = cacheContext.get(uri);
			if (context != null) return context;
		}
		MUri muri = MUri.toUri(uri);
		try {
			EProcess process = engine.getProcess(muri);
			EPool pool = engine.getPool(process, muri);
			EngineContext context = new EngineContext(engine);
			context.setEProcess(process);
			context.setEPool(pool);
			synchronized (cacheContext) {
				cacheContext.put(uri, context);
			}
			return context;
		} catch (Throwable t) {
			log().e(uri,user,t);
			return null;
		}
	}

	public boolean hasReadAccess(String uri) {	
		synchronized (cacheAccess) {
			Boolean hasAccess = cacheAccess.get(uri);
			if (hasAccess != null) return hasAccess;
		}

		boolean hasAccess = engine.hasReadAccess(uri, user);
		synchronized (cacheAccess) {
			cacheAccess.put(uri,hasAccess);
		}
		return hasAccess;
	}
		

	
}