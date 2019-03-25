package org.dhis2.utils;

import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ParentChildModelTest {

    @Test
    public void testCreateParentChildModel() {
        OrganisationUnit orgToAdd1 = OrganisationUnit.builder()
                .uid("XXXX1")
                .level(1)
                .parent(OrganisationUnit.builder().uid("XXXX2").build())
                .name("Path name")
                .displayName("Display name")
                .displayShortName("Display short name")
                .build();

        OrganisationUnit orgToAdd2 = OrganisationUnit.builder()
                .uid("XXXX3")
                .level(1)
                .parent(OrganisationUnit.builder().uid("XXXX4").build())
                .name("Path name")
                .displayName("Display name")
                .displayShortName("Display short name")
                .build();

        ParentChildModel<OrganisationUnit> orgUnitParent1 =
                ParentChildModel.create(orgToAdd1, new ArrayList<>(), true);

        ParentChildModel<OrganisationUnit> orgUnitParent2 =
                ParentChildModel.create(orgToAdd2, new ArrayList<>(), true);

        List<ParentChildModel<OrganisationUnit>> parentChildModels = new ArrayList<>();
        parentChildModels.add(orgUnitParent1);
        parentChildModels.add(orgUnitParent2);

        ParentChildModel<OrganisationUnit> orgUnitParent3 =
                ParentChildModel.create(orgToAdd2, parentChildModels, true);


        assertNotNull(orgUnitParent1);
        assertTrue(orgUnitParent1.isSelectable());

        orgUnitParent1.addItem(orgUnitParent2);

        assertEquals(1, orgUnitParent1.childs().size());

        assertEquals(2, orgUnitParent3.childs().size());
    }
}
