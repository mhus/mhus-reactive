package de.mhus.cherry.reactive.engine.mockup;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import de.mhus.cherry.reactive.model.engine.PCase;
import de.mhus.cherry.reactive.model.engine.PCase.STATE_CASE;
import de.mhus.cherry.reactive.model.engine.PNode;
import de.mhus.cherry.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.cherry.reactive.model.engine.PNode.TYPE_NODE;
import de.mhus.lib.core.MProperties;
import de.mhus.lib.core.config.IConfig;
import de.mhus.lib.core.directory.WritableResourceNode;
import de.mhus.lib.errors.MException;
import de.mhus.lib.errors.NotFoundException;

public class Step {
	
	LinkedList<PCase> cases = new LinkedList<>();
	LinkedList<PNode> nodes = new LinkedList<>();
	private int nr;
	
	public Step() {
	}
	
	public Step(IConfig n) throws MException {
		nr = n.getInt("nr", 0);
		for (IConfig nc : n.getNode("cases").getNodes()) {
			
			MProperties p = new MProperties();
			IConfig nps = nc.getNode("parameters");
			for (String key : nps.getPropertyKeys())
				p.setString(key, nps.getString(key));
			
			PCase caze = new PCase(
					UUID.randomUUID(),
					new MProperties(),
					nc.getString("uri"),
					nc.getString("name"),
					nc.getString("canonical"),
					0,
					null,
					STATE_CASE.valueOf(nc.getString("state")),
					nc.getLong("scheduled", 0),
					null,
					p
					);
			cases.add(caze);
		}
		
		for (IConfig nc : n.getNode("nodes").getNodes()) {
			
			MProperties p = new MProperties();
			IConfig nps = nc.getNode("parameters");
			for (String key : nps.getPropertyKeys())
				p.setString(key, nps.getString(key));
			
			PNode node = new PNode(
					UUID.randomUUID(),
					UUID.randomUUID(),
					nc.getString("name"),
					nc.getString("canonical"),
					0,
					0,
					STATE_NODE.valueOf(nc.getString("state")),
					STATE_NODE.valueOf(nc.getString("suspended")),
					null,
					null,
					null,
					false,
					TYPE_NODE.valueOf(nc.getString("type")),
					null,
					p,
					nc.getString("assigned"),
					UUID.randomUUID(),
					nc.getInt("tryCount", 0)
					);
			
			nodes.add(node);
		}
		
	}

	public void add(PCase caze) {
		cases.add(new PCase(caze));
	}

	public void add(PNode node) {
		nodes.add(new PNode(node));
	}

	public void save(int nr, IConfig n) throws MException {
		n.setInt("nr", nr);
		WritableResourceNode<IConfig> ncs = n.createConfig("cases");
		for (PCase caze :cases) {
			WritableResourceNode<IConfig> nc = ncs.createConfig(caze.getCanonicalName());
			nc.setString("state", caze.getState().name());
			nc.setString("name", caze.getName());
			nc.setString("canonical", caze.getCanonicalName());
			nc.setString("uri", caze.getUri());
			nc.setLong("scheduled", caze.getScheduled());
			WritableResourceNode<IConfig> np = nc.createConfig("parameters");
			for (Entry<String, Object> entry : caze.getParameters().entrySet())
				if (entry.getValue().getClass().isPrimitive() || entry.getValue().getClass() == String.class)
					np.setString(entry.getKey(), String.valueOf(entry.getValue()));
		}
		
		WritableResourceNode<IConfig> nns = n.createConfig("nodes");
		for (PNode node : nodes) {
			WritableResourceNode<IConfig> nn = nns.createConfig(node.getCanonicalName());
			nn.setString("state", node.getState().name());
			nn.setString("suspended", node.getState().name());
			nn.setString("type", node.getType().name());
			nn.setString("assigned", String.valueOf(node.getAssignedUser()));
			nn.setString("name", node.getName());
			nn.setString("canonical", node.getCanonicalName());
			nn.setInt("tryCount", node.getTryCount());

			WritableResourceNode<IConfig> np = nn.createConfig("parameters");
			for (Entry<String, Object> entry : node.getParameters().entrySet())
				if (entry.getValue().getClass().isPrimitive() || entry.getValue().getClass() == String.class)
					np.setString(entry.getKey(), String.valueOf(entry.getValue()));
		}
		
		
	}

	public void check(boolean warn,boolean verbose, int cnt, PCase caze) throws NotFoundException {
		Iterator<PCase> iter = cases.iterator();
		while(iter.hasNext()) {
			PCase c = iter.next();
			boolean b = compare(c,caze); 
			if (b) {
				iter.remove();
				if (verbose) {
					System.out.println(cnt+" --- CASE FOUND: " + toString(caze));
					System.out.println(cnt+"     CASE FOUND: " + toString(c));
				}
				return;
			}
		}
		System.err.println(cnt+" *** CASE NOT FOUND: " + toString(caze));
		for (PCase c : cases) {
			System.err.println("    LEFT: " + toString(c));
		}
		if (!warn)
			throw new NotFoundException("case not found",toString(caze));
	}

	public void check(boolean warn, boolean verbose, int cnt, PNode node) throws NotFoundException {
		if (node.getType() == TYPE_NODE.RUNTIME) return;
		Iterator<PNode> iter = nodes.iterator();
		while(iter.hasNext()) {
			PNode c = iter.next();
			boolean b = compare(c,node); 
			if (b) {
				iter.remove();
				if (verbose) {
					System.out.println(cnt+" --- NODE FOUND: " + toString(node));
					System.out.println(cnt+"     NODE FOUND: " + toString(c));
				}
				return;
			}
		}
		System.err.println(cnt+" *** NODE NOT FOUND: " + toString(node));
		for (PNode n : nodes) {
			System.err.println("    LEFT: " + toString(n));
		}
		if (!warn)
			throw new NotFoundException("node not found",toString(node));
	}
	
	private String toString(PNode node) {
		return node.getState() +" "+node.getCanonicalName()+" "+node.getType()+" "+node.getTryCount()+" "+node.getParameters();
	}

	private String toString(PCase caze) {
		return caze.getState() +" "+caze.getCanonicalName()+" "+caze.getParameters();
	}
	
	private boolean compare(PCase c, PCase caze) {
		
		return  c.getState() == caze.getState() 
				&& 
				c.getCanonicalName().equals(caze.getCanonicalName())
				&&
				compare(c.getParameters(),caze.getParameters())
				;
	}

	private boolean compare(PNode c, PNode node) {
		return  c.getState() == node.getState() 
				&& 
				c.getCanonicalName().equals(node.getCanonicalName())
				&&
				compare(c.getParameters(),node.getParameters())
				&&
				c.getType() == node.getType()
				&&
				c.getTryCount() == node.getTryCount()
				;
	}

	private boolean compare(Map<String, Object> p1, Map<String, Object> p2) {
		for (Entry<String, Object> e2 : p2.entrySet()) {
			if (e2.getValue().getClass().isPrimitive() || e2.getValue().getClass() == String.class) {
				if (p1.get(e2.getKey()) == null) continue; // not recorded
				String v1 = String.valueOf(p1.get(e2.getKey()));
				String v2 = String.valueOf(e2.getValue());
				
				if (!v1.equals(v2)) 
					return false;
			}
		}
		return true;
	}

	public int getNr() {
		return nr;
	}

}
