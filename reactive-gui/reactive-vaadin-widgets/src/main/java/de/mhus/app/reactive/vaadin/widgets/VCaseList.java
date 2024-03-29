/**
 * Copyright (C) 2018 Mike Hummel (mh@mhus.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mhus.app.reactive.vaadin.widgets;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.vaadin.event.Action;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.v7.event.ItemClickEvent;
import com.vaadin.v7.event.ItemClickEvent.ItemClickListener;

import de.mhus.app.reactive.model.engine.EngineConst;
import de.mhus.app.reactive.model.engine.PCase.STATE_CASE;
import de.mhus.app.reactive.model.engine.SearchCriterias;
import de.mhus.app.reactive.model.engine.SearchCriterias.ORDER;
import de.mhus.app.reactive.model.ui.ICase;
import de.mhus.app.reactive.model.ui.IEngine;
import de.mhus.app.reactive.model.util.CaseActionList;
import de.mhus.lib.core.MSystem;
import de.mhus.lib.core.logging.Log;
import de.mhus.lib.core.util.MNls;
import de.mhus.lib.core.util.MNlsFactory;
import de.mhus.lib.vaadin.ExpandingTable;
import de.mhus.lib.vaadin.MhuTable;
import de.mhus.lib.vaadin.container.MhuBeanItemContainer;

@SuppressWarnings("deprecation")
public class VCaseList extends MhuTable implements Refreshable {

    private Log log = Log.getLog(VCaseList.class);
    private static final long serialVersionUID = 1L;
    protected static final Action ACTION_REFRESH = new Action("Refresh");
    protected static final Action ACTION_ARCHIVE = new Action("Archive");
    protected static final Action ACTION_DETAILS = new Action("Details");
    protected static final Action ACTION_RUNTIME = new Action("Runtime");
    protected static final Action ACTION_ACTIONS = new Action("Actions");
    protected static final Action ACTION_NODES = new Action("Nodes");
    private String sortByDefault = EngineConst.FIELD_DUEDATE;
    private boolean sortAscDefault = true;
    MhuBeanItemContainer<CaseItem> data = new MhuBeanItemContainer<CaseItem>(CaseItem.class);
    private SearchCriterias criterias;
    private String[] properties;

    private int lastExtend;
    private int page;
    private IEngine engine;
    private int size = 100;
    private WidgetActivityDelegate activity;

    public VCaseList() {}

    public VCaseList(WidgetActivityDelegate activity) {
        this.activity = activity;
    }

    public void configure(
            IEngine engine,
            WidgetActivityDelegate activity,
            SearchCriterias criterias,
            String[] properties) {
        this.engine = engine;
        this.criterias = criterias;
        this.properties = properties;
        this.activity = activity;

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

        addItemClickListener(
                new ItemClickListener() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void itemClick(ItemClickEvent event) {
                        if (event.isDoubleClick()) {
                            @SuppressWarnings("unused")
                            CaseItem selected = (CaseItem) event.getItemId();
                            //					Notification.show("DoubleClick: " +
                            // ((NodeItem)event.getItemId()).getName());
                        }
                    }
                });

        addActionHandler(
                new Action.Handler() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Action[] getActions(Object target, final Object sender) {
                        LinkedList<Action> list = new LinkedList<>();
                        Collection<?> targets = getSelectedValues();
                        if (targets != null && targets.size() > 0)
                            target = targets.iterator().next();

                        if (target != null) {
                            CaseItem caze = (CaseItem) target;
                            showActions(list, caze);
                        }
                        return list.toArray(new Action[list.size()]);
                    }

                    @Override
                    public void handleAction(
                            final Action action, final Object sender, final Object target) {
                        try {
                            //					INode node =
                            // engine.getNode(((NodeItem)target).getId().toString(),null);
                            CaseItem caze = (CaseItem) target;
                            doAction(action, caze);
                        } catch (Throwable t) {
                            log.e(t);
                        }
                    }
                });

        setDragMode(TableDragMode.NONE);
        setMultiSelect(false);
        renderEventHandler()
                .register(
                        new RenderListener() {
                            @Override
                            public void onRender(ExpandingTable mhuTable, int first, int last) {
                                doExtendTable(mhuTable, first, last);
                            }
                        });

        sortEventHandler()
                .register(
                        new SortListener() {
                            @Override
                            public void onSortChanged(ExpandingTable mhuTable) {
                                doReload();
                            }
                        });
        setImmediate(true);
    }

    protected void doAction(Action action, CaseItem caze) throws Exception {
        if (action == ACTION_REFRESH) {
            doReload();
        } else if (action == ACTION_ARCHIVE) {
            activity.doCaseArchive(caze.getId());
            doReload();
        } else if (action == ACTION_DETAILS) {
            activity.showCaseDetails(caze.getId());
        } else if (action == ACTION_RUNTIME) {
            activity.showCaseRuntime(caze.getId());
        } else if (action == ACTION_ACTIONS) {
            doActions(caze.getId());
        } else if (action == ACTION_NODES) {
            activity.showCaseNodes(caze.getId());
        }
    }

    protected void showActions(LinkedList<Action> list, CaseItem caze) {
        if (activity.isShowCaseDetails(caze.getId())) list.add(ACTION_DETAILS);
        if (activity.isShowCaseActions(caze.getId())) list.add(ACTION_ACTIONS);
        if (activity.isShowCaseRefresh(caze.getId())) list.add(ACTION_REFRESH);
        if (activity.isShowCaseRuntime(caze.getId())) list.add(ACTION_RUNTIME);
        if (activity.isShowCaseArchive(caze.getId()))
            if (caze.getState() == STATE_CASE.CLOSED) list.add(ACTION_ARCHIVE);
        if (activity.isShowCaseNodes(caze.getId())) list.add(ACTION_NODES);
    }

    protected void doActions(UUID caseId) {
        try {
            CaseActionList actions = new CaseActionList(engine, caseId.toString());
            if (!actions.isValid()) {
                Notification.show("Keine Aktionen gefunden", Type.TRAY_NOTIFICATION);
                return;
            }
            CaseActionDialog dialog = new CaseActionDialog(actions, activity);
            dialog.show(getUI());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void doReload() {
        data.removeAllItems();
        doRefresh(0);
    }

    private CaseContainer getItems(int page) {

        try {
            criterias.order =
                    ORDER.valueOf(String.valueOf(getSortContainerPropertyId()).toUpperCase());
            criterias.orderAscending = isSortAscending();
        } catch (Throwable t) {
        }

        CaseContainer out = new CaseContainer();
        try {
            List<ICase> list = engine.searchCases(criterias, page, size, properties);
            for (ICase item : list) out.addItem(new CaseItem(engine, item));
        } catch (Exception e) {
            log.w(e);
            Notification.show("Liste konnten nicht abgefragt werden", Type.WARNING_MESSAGE);
        }

        return out;
    }

    private void sortTable() {
        sort(new Object[] {getSortContainerPropertyId()}, new boolean[] {isSortAscending()});
    }

    protected void doExtendTable(ExpandingTable mhuTable, int first, int last) {
        int size = mhuTable.getItemIds().size() - 1;
        if (lastExtend < last && last == size) {
            lastExtend = last;
            doRefresh(++page);
        }
    }

    protected void doRefresh(int page_) {

        CaseContainer updatedData = getItems(page_);
        if (updatedData != null) {
            if (data == null) data = updatedData;

            data.mergeAll(
                    updatedData.getItemIds(),
                    page_ == 0 ? true : false,
                    new Comparator<CaseItem>() {
                        @Override
                        public int compare(CaseItem o1, CaseItem o2) {
                            return MSystem.equals(o1, o2) ? 0 : 1;
                        }
                    });
        } else {
            Notification.show(
                    "Daten konnten nicht abgefragt werden", Notification.Type.WARNING_MESSAGE);
            if (data == null) data = new CaseContainer();
            return;
        }
        //		sortTable();
        if (page_ == 0) {
            lastExtend = 0;
            page = 0;
            setCurrentPageFirstItemIndex(0);
            Notification.show("Liste aktualisiert", Notification.Type.TRAY_NOTIFICATION);
        }
    }

    public void setSearchCriterias(SearchCriterias c) {
        if (c != null) criterias = c;
        doReload();
    }

    public SearchCriterias getSearchCriterias() {
        return criterias;
    }

    @Override
    public void doRefresh() {
        doReload();
    }
}
