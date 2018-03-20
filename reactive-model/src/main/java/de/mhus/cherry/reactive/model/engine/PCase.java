package de.mhus.cherry.reactive.model.engine;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PCase implements Externalizable {
	
	public enum STATE_CASE {NEW, RUNNING, SUSPENDED, CLOSED};
	
	protected UUID id;
	protected String uri;
	protected String name;
	protected long creationDate;
	protected String createdBy;
	protected STATE_CASE state = STATE_CASE.NEW;
	protected long scheduled;
	protected Map<String,Object> options;
	protected Map<String,Object> parameters;
	private String canonicalName;
	private int closedCode = 0;
	private String closedMessage;

	public PCase() {}

	public PCase(PCase clone) {
		this.id = clone.getId();
		this.options = clone.getOptions();
		this.uri = clone.getUri();
		this.name = clone.getName();
		this.canonicalName = clone.getCanonicalName();
		this.creationDate = clone.getCreationDate();
		this.createdBy = clone.getCreatedBy();
		this.state = clone.getState();
		this.scheduled = clone.getScheduled();
		this.parameters = new HashMap<>(clone.getParameters());
		this.closedCode = clone.getClosedCode();
		this.closedMessage = clone.getClosedMessage();
	}
	
	public PCase(UUID id, Map<String,Object> options, String uri, String name, String canonicalName, long creationDate, String createdBy, STATE_CASE state,
	        long scheduled, Map<String, Object> parameters) {
		this.id = id;
		this.options = new HashMap<>(options);
		this.uri = uri;
		this.name = name;
		this.canonicalName = canonicalName;
		this.creationDate = creationDate;
		this.createdBy = createdBy;
		this.state = state;
		this.scheduled = scheduled;
		this.parameters = new HashMap<>(parameters);
	}

	public UUID getId() {
		return id;
	}
	
	public String getCustomId() {
		return (String)options.getOrDefault(EngineConst.OPTION_CUSTOM_ID, "");
	}

	public String getUri() {
		return uri;
	}

	public String getName() {
		return name;
	}

	public String getCanonicalName() {
		return canonicalName;
	}
	
	public long getCreationDate() {
		return creationDate;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public STATE_CASE getState() {
		return state;
	}

	public long getScheduled() {
		return scheduled;
	}

	public Map<String, Object> getParameters() {
		if (parameters == null) parameters = new HashMap<>();
		return parameters;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(1);
		
		out.writeObject(id);
		out.writeObject(options);
		out.writeObject(uri);
		out.writeObject(name);
		out.writeObject(canonicalName);
		
		out.writeLong(creationDate);
		out.writeObject(createdBy);
		out.writeObject(state);
		out.writeLong(scheduled);
		out.writeObject(parameters);
		
		out.writeInt(closedCode);
		out.writeObject(closedMessage);
		out.flush();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		
		int version = in.readInt();
		if (version != 1) throw new IOException("Wrong version: " + version);
		
		id = (UUID) in.readObject();
		options = (Map<String, Object>) in.readObject();
		uri = (String) in.readObject();
		name = (String) in.readObject();
		canonicalName = (String) in.readObject();
		
		creationDate = in.readLong();
		createdBy = (String) in.readObject();
		state = (STATE_CASE) in.readObject();
		scheduled = in.readLong();
		parameters = (Map<String, Object>) in.readObject();
		
		closedCode = in.readInt();
		closedMessage = (String) in.readObject();
		
	}

	public void setState(STATE_CASE state) {
		if (this.state == STATE_CASE.CLOSED) return;
		this.state = state;
	}

	@Override
	public String toString() {
		return "PCase: " + getCanonicalName() + " " + state + " " + id ;
	}

	public Map<String,Object> getOptions() {
		return Collections.unmodifiableMap(options);
	}
	
	public void close(int code, String message) {
		if (this.state == STATE_CASE.CLOSED) return;
		closedCode = code;
		closedMessage = message;
		setState(STATE_CASE.CLOSED);
	}
	
	public String getClosedMessage() {
		return closedMessage;
	}

	public int getClosedCode() {
		return closedCode;
	}
	
	public boolean isFailed() {
		return state == STATE_CASE.CLOSED && closedCode < 0;
	}

}