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

import java.util.Date;
import java.util.UUID;

import de.mhus.app.reactive.model.engine.PCase.STATE_CASE;
import de.mhus.app.reactive.model.ui.ICase;
import de.mhus.app.reactive.model.ui.IEngine;
import de.mhus.lib.annotations.vaadin.Column;
import de.mhus.lib.core.util.MUri;
import de.mhus.lib.errors.MException;

public class CaseItem {

    private ICase caze;
    private IEngine engine;
    private MUri uri;

    public CaseItem(IEngine engine, ICase caze) {
        this.engine = engine;
        this.caze = caze;
        uri = MUri.toUri(caze.getUri());
    }

    @Column(order = 1, title = "Name", editable = false)
    public String getDisplayName() {
        try {
            return engine.getProcess(caze.getUri()).getDisplayName(caze.getUri(), null);
        } catch (MException e) {
            return "?";
        }
    }

    @Column(order = 2, title = "CutsomId", editable = false)
    public String getCustom() {
        return caze.getCustomId();
    }

    @Column(order = 3, title = "Milestone", editable = false)
    public String getMilestone() {
        return caze.getMilestone();
    }

    @Column(order = 4, title = "CName", editable = false, elapsed = false)
    public String getName() {
        return caze.getCanonicalName();
    }

    @Column(order = 5, title = "Index 1", editable = false)
    public String getIndex0() {
        return caze.getProperties().get("pnode.index0");
    }

    @Column(order = 6, title = "Index 2", editable = false)
    public String getIndex1() {
        return caze.getProperties().get("pnode.index1");
    }

    @Column(order = 7, title = "Index 3", editable = false)
    public String getIndex2() {
        return caze.getProperties().get("pnode.index2");
    }

    @Column(order = 8, title = "Index 4", editable = false)
    public String getIndex3() {
        return caze.getProperties().get("pnode.index3");
    }

    @Column(order = 9, title = "Index 5", editable = false)
    public String getIndex4() {
        return caze.getProperties().get("pnode.index4");
    }

    @Column(order = 10, title = "Index 6", editable = false, elapsed = false)
    public String getIndex5() {
        return caze.getProperties().get("pnode.index5");
    }

    @Column(order = 11, title = "Index 7", editable = false, elapsed = false)
    public String getIndex6() {
        return caze.getProperties().get("pnode.index6");
    }

    @Column(order = 12, title = "Index 8", editable = false, elapsed = false)
    public String getIndex7() {
        return caze.getProperties().get("pnode.index7");
    }

    @Column(order = 13, title = "Index 9", editable = false, elapsed = false)
    public String getIndex8() {
        return caze.getProperties().get("pnode.index8");
    }

    @Column(order = 14, title = "Index 10", editable = false, elapsed = false)
    public String getIndex9() {
        return caze.getProperties().get("pnode.index9");
    }

    @Column(order = 15, title = "State", editable = false)
    public STATE_CASE getState() {
        return caze.getState();
    }

    @Column(order = 16, title = "URI", editable = false, elapsed = false)
    public String getUri() {
        return caze.getUri();
    }

    @Column(order = 17, title = "Customer", editable = false)
    public String getCustomer() {
        return caze.getCustomerId();
    }

    @Column(order = 18, title = "Process", editable = false, elapsed = false)
    public String getProcess() {
        return uri.getLocation();
    }

    @Column(order = 19, title = "Pool", editable = false, elapsed = false)
    public String getPool() {
        return uri.getPath();
    }

    @Column(order = 20, title = "id", editable = false, elapsed = false)
    public UUID getId() {
        return caze.getId();
    }

    @Column(order = 21, title = "Created", editable = false, elapsed = true)
    public Date getCreated() {
        return new Date(caze.getCreated());
    }

    @Column(order = 22, title = "Modified", editable = false, elapsed = false)
    public Date getModified() {
        return new Date(caze.getModified());
    }

    @Override
    public boolean equals(Object in) {
        if (in == null || !(in instanceof CaseItem)) return false;
        return caze.getId().equals(((CaseItem) in).getId());
    }
}
