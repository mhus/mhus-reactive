package de.mhus.cherry.reactive.vaadin.widgets;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.vaadin.event.Action;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Table.TableDragMode;

import de.mhus.cherry.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.cherry.reactive.model.engine.PNode.TYPE_NODE;
import de.mhus.cherry.reactive.model.engine.SearchCriterias;
import de.mhus.cherry.reactive.model.engine.SearchCriterias.ORDER;
import de.mhus.cherry.reactive.model.ui.IEngine;
import de.mhus.cherry.reactive.model.ui.INode;
import de.mhus.lib.core.MSystem;
import de.mhus.lib.core.logging.Log;
import de.mhus.lib.core.util.MNls;
import de.mhus.lib.core.util.MNlsFactory;
import de.mhus.lib.vaadin.ExpandingTable;
import de.mhus.lib.vaadin.MhuTable;
import de.mhus.lib.vaadin.container.MhuBeanItemContainer;

public class VNodeList extends MhuTable {

	private Log log = Log.getLog(VNodeList.class);
	private static final long serialVersionUID = 1L;
	protected static final Action ACTION_ASSIGN = new Action("Assign");
	protected static final Action ACTION_UNASSIGN = new Action("Unassign");
	protected static final Action ACTION_EXECUTE = new Action("Do it");
	protected static final Action ACTION_REFRESH = new Action("Refresh");
	private String sortByDefault = "duedate";
	private boolean sortAscDefault = true;
	MhuBeanItemContainer<NodeItem> data = new MhuBeanItemContainer<NodeItem>(NodeItem.class);
	private SearchCriterias criterias;
	private String[] properties;
	
	private int lastExtend;
	private int page;
	private IEngine engine;
	private int size = 100;

	public VNodeList() {
	}
	
	public void configure(IEngine engine, SearchCriterias criterias, String[] properties) {
		this.engine = engine;
		this.criterias = criterias;
		this.properties = properties;
	
		
		data = getItems(0);
        setSizeFull();
        addStyleName("borderless");
        setSelectable(true);
        setTableEditable(false);
        setColumnCollapsingAllowed(true);
        setColumnReorderingAllowed(true);
        setSortContainerPropertyId(sortByDefault);
        setSortAscending(sortAscDefault);
        if (data != null) {
        	data.removeAllContainerFilters();
        	setContainerDataSource(data);
        	MNls nls = new MNlsFactory().create(this);
        	data.configureTableByAnnotations(this, null, nls);
        }
        sortTable();
        
        addItemClickListener(new ItemClickListener() {
			private static final long serialVersionUID = 1L;
			@Override
			public void itemClick(ItemClickEvent event) {
				if (event.isDoubleClick()) {
					NodeItem selected = (NodeItem)event.getItemId();
					if (selected != null && selected.getState() == STATE_NODE.WAITING && selected.getType() == TYPE_NODE.HUMAN) {
						doOpenHumanForm(selected);
					}
//					Notification.show("DoubleClick: " + ((NodeItem)event.getItemId()).getName());
				}
			}
		});
        
        addActionHandler(new Action.Handler() {
			private static final long serialVersionUID = 1L;
			@Override
            public Action[] getActions(Object target, final Object sender) {
				LinkedList<Action> list = new LinkedList<>();
				Collection<?> targets = getSelectedValues();
				if (targets != null && targets.size() > 0)
					target = targets.iterator().next();
				
				if (target != null) {
					NodeItem node = (NodeItem)target;
					if (node.getState() == STATE_NODE.WAITING && node.getType() == TYPE_NODE.HUMAN) {
						if (node.getAssigned() == null) {
							list.add(ACTION_ASSIGN);
						} else {
							list.add(ACTION_UNASSIGN);
						}
						list.add(ACTION_EXECUTE);
					}
					list.add(ACTION_REFRESH);
				}
				return list.toArray(new Action[list.size()]);
			}
            @Override
            public void handleAction(final Action action, final Object sender,
                    final Object target) {
            	try {
					INode node = engine.getNode(((NodeItem)target).getId().toString(),null);
					
	            	if (action == ACTION_UNASSIGN) {
	            		node.doUnassign();
	            		doReload();
	            	} else
	            	if (action == ACTION_ASSIGN) {
	            		node.doAssign();
	            		doReload();
	            	} else
	            	if (action == ACTION_EXECUTE) {
	            		doOpenHumanForm((NodeItem)target);
	            	} else
	            	if (action == ACTION_REFRESH) {
	            		doReload();
	            	}
				} catch (Throwable t) {
					log.e(t);
				}
            }
        });
        
        setDragMode(TableDragMode.NONE);
        setMultiSelect(false);
        renderEventHandler().register(new RenderListener() {
			@Override
			public void onRender(ExpandingTable mhuTable, int first, int last) {
				doExtendTable(mhuTable, first, last);
			}
		});

        sortEventHandler().register(new SortListener() {
			@Override
			public void onSortChanged(ExpandingTable mhuTable) {
				doReload();
			}
		});
        setImmediate(true);
		
	}

	public void doReload() {
		data.removeAllItems();
		doRefresh(0);
	}

	protected void doOpenHumanForm(NodeItem selected) {
		
	}

	private NodeContainer getItems(int page) {
		
		try {
			criterias.order = ORDER.valueOf(String.valueOf(getSortContainerPropertyId()).toUpperCase());
			criterias.orderAscending = isSortAscending();
		} catch (Throwable t) {}
		
		NodeContainer out = new NodeContainer();
		try {
			List<INode> list = engine.searchNodes(criterias, page, size , properties);
			for (INode item : list)
				out.addItem(new NodeItem(engine,item));
		} catch (Exception e) {
			log.w(e);
			Notification.show("Liste konnten nicht abgefragt werden", Type.WARNING_MESSAGE);
		}
		
		return out;
	}
	
	private void sortTable() {
		sort(new Object[] { getSortContainerPropertyId() }, new boolean[] { isSortAscending() });
	}

	protected void doExtendTable(ExpandingTable mhuTable, int first, int last) {
		int size = mhuTable.getItemIds().size() - 1;
		if (lastExtend < last && last == size) {
			lastExtend = last;
			doRefresh(++page);
		}
	}

	protected void doRefresh(int page_) {
		
		
		NodeContainer updatedData = getItems(page_);
		if (updatedData != null) {
			if (data == null)
				data = updatedData;
			
			data.mergeAll(updatedData.getItemIds(), page_ == 0 ? true : false, new Comparator<NodeItem>() {
				@Override
				public int compare(NodeItem o1, NodeItem o2) {
					return MSystem.equals(o1, o2) ? 0 : 1;
				}
			});
		}
		else {
			Notification.show("Daten konnten nicht abgefragt werden",Notification.Type.WARNING_MESSAGE);
			if (data == null)
				data = new NodeContainer();
			return;
		}
//		sortTable();
		if (page_ == 0) {
			lastExtend = 0;
			page = 0;
			setCurrentPageFirstItemIndex(0);
			Notification.show("Liste aktualisiert",Notification.Type.TRAY_NOTIFICATION);
		}
	}

}
