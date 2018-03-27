package de.mhus.cherry.reactive.model.engine;

import java.util.UUID;

import de.mhus.cherry.reactive.model.engine.PCase.STATE_CASE;

public class PCaseInfo {

	private UUID id;
	private String uri;
	private String canonicalName;
	private STATE_CASE state;
	private String customId;
	private String[] indexValues;
	private String customerId;
	private long created;
	private long modified;
	private int priority;
	private int score;

	public PCaseInfo(
			UUID id, 
			String uri, 
			String canonicalName, 
			STATE_CASE state, 
			String customId, 
			String customerId, 
			long created,
			long modified,
			int priority,
			int score,
			String[] indexValues) {
		this.id = id;
		this.uri = uri;
		this.canonicalName = canonicalName;
		this.state = state;
		this.customId = customId;
		this.customerId = customerId;
		this.indexValues = indexValues;
		this.created = created;
		this.modified = modified;
		this.priority = priority;
		this.score = score;
	}

	public PCaseInfo(PCase caze) {
		this(
				caze.getId(),
				caze.getUri(),
				caze.getCanonicalName(),
				caze.getState(), 
				caze.getCustomId(), 
				caze.getCustomerId(), 
				0,
				0,
				0,
				0,
				null);
	}

	public UUID getId() {
		return id;
	}
	
	public String getUri() {
		return uri;
	}

	public String getCanonicalName() {
		return canonicalName;
	}
	
	public STATE_CASE getState() {
		return state;
	}

	public String getCustomId() {
		return customId;
	}
	
	public String getCustomerId() {
		return customerId;
	}
	
	public String getIndexValue(int index) {
		if (indexValues == null || index < 0 || index >= indexValues.length) return null;
		return indexValues[index];
	}
	
	public long getCreated() {
		return created;
	}

	public long getModified() {
		return modified;
	}

	public int getPriority() {
		return priority;
	}

	public int getScore() {
		return score;
	}

}
