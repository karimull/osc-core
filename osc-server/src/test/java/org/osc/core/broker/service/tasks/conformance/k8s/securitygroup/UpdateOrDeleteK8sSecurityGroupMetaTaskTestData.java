/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.service.tasks.conformance.k8s.securitygroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.Task;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.FailurePolicyType;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.k8s.Label;
import org.osc.core.broker.model.entities.virtualization.k8s.Pod;
import org.osc.core.broker.model.entities.virtualization.k8s.PodPort;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.CheckPortGroupHookMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.DeleteSecurityGroupFromDbTask;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.PortGroupCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.SecurityGroupMemberMapPropagateMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroupinterface.DeleteSecurityGroupInterfaceTask;
import org.osc.core.broker.service.tasks.conformance.securitygroupinterface.MarkSecurityGroupInterfaceDeleteTask;
import org.osc.core.common.job.TaskGuard;
import org.osc.core.common.virtualization.VirtualizationType;

public class UpdateOrDeleteK8sSecurityGroupMetaTaskTestData {

    public final static String MGR_TYPE = "SMC";
    private final static int MANY = 4;

    public static SecurityGroup NO_LABEL_SG = createSecurityGroup("NO_LABEL", 0, 0, false);
    public static SecurityGroup SINGLE_LABEL_SG = createSecurityGroup("SINGLE_LABEL", 1, 0, false);
    public static SecurityGroup MULTI_LABEL_SG = createSecurityGroup("MULTI_LABEL", MANY, 0, false);
    public static SecurityGroup WTIH_SGIS_SG = createSecurityGroup("WTIH_SGIS_SG", MANY, MANY, false);
    public static SecurityGroup SINGLE_LABEL_MARKED_FOR_DELETION_SG = createSecurityGroupWithPod("POPULATED_WITH_POD_SG", 0, true);
    public static SecurityGroup WITH_SGIS_MARKED_FOR_DELETION_SG = createSecurityGroupWithPod("WITH_SGIS_MARKED_FOR_DELETION_SG", MANY, true);

    public static TaskGraph createK8sGraph(SecurityGroup sg, boolean isDelete) {
        TaskGraph expectedGraph = new TaskGraph();

        for (SecurityGroupMember sgm : sg.getSecurityGroupMembers()) {
            expectedGraph.addTask(new CheckK8sSecurityGroupLabelMetaTask().create(sgm, isDelete));
        }

        expectedGraph.appendTask(new PortGroupCheckMetaTask().create(sg, isDelete, null), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        expectedGraph.appendTask(new SecurityGroupMemberMapPropagateMetaTask().create(sg));

        sg.getSecurityGroupInterfaces().forEach(sgi -> expectedGraph.appendTask(new CheckPortGroupHookMetaTask().create(sgi, false)));

        return expectedGraph;
    }

    public static TaskGraph deleteSGK8sGraph(SecurityGroup sg, boolean isDelete) {
        TaskGraph expectedGraph = new TaskGraph();

        List<Task> tasksToPreceedDeleteSGI = new ArrayList<>();

        for (SecurityGroupMember sgm : sg.getSecurityGroupMembers()) {
            expectedGraph.addTask(new CheckK8sSecurityGroupLabelMetaTask().create(sgm, isDelete));
        }

        SecurityGroupMember sgm = sg.getSecurityGroupMembers().iterator().next();
        String domainId = sgm.getPodPorts().iterator().next().getParentId();

        expectedGraph.appendTask(new PortGroupCheckMetaTask().create(sg, isDelete, domainId), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        sg.getSecurityGroupInterfaces().forEach(sgi -> expectedGraph.addTask(new MarkSecurityGroupInterfaceDeleteTask().create(sgi)));

        SecurityGroupMemberMapPropagateMetaTask securityGroupMemberMapPropagateMetaTask = new SecurityGroupMemberMapPropagateMetaTask().create(sg);
        tasksToPreceedDeleteSGI.add(securityGroupMemberMapPropagateMetaTask);
        expectedGraph.appendTask(securityGroupMemberMapPropagateMetaTask);

        sg.getSecurityGroupInterfaces().forEach(sgi -> {
            CheckPortGroupHookMetaTask checkPortGroupHookMetaTask = new CheckPortGroupHookMetaTask().create(sgi, true);
            tasksToPreceedDeleteSGI.add(checkPortGroupHookMetaTask);
            expectedGraph.appendTask(checkPortGroupHookMetaTask);
        });

        sg.getSecurityGroupInterfaces().forEach(sgi -> expectedGraph.addTask(new DeleteSecurityGroupInterfaceTask().create(sgi), tasksToPreceedDeleteSGI.toArray(new Task[0])));

        expectedGraph.appendTask(new DeleteSecurityGroupFromDbTask().create(sg));

        return expectedGraph;
    }

    public static void persistObjects(EntityManager em, SecurityGroup sg) {
        Set<VirtualSystem> virtualSystems = sg.getVirtualizationConnector().getVirtualSystems();

        em.getTransaction().begin();

        em.persist(sg.getVirtualizationConnector());

        for (VirtualSystem vs : virtualSystems) {
            em.persist(vs.getDomain().getApplianceManagerConnector());
            em.persist(vs.getApplianceSoftwareVersion().getAppliance());
            em.persist(vs.getApplianceSoftwareVersion());
            em.persist(vs.getDistributedAppliance());
            em.persist(vs.getDomain());
            em.persist(vs);
        }

        em.persist(sg);
        for (SecurityGroupMember sgm : sg.getSecurityGroupMembers()) {
            if (!sgm.getPodPorts().isEmpty()) {
                for (Pod pod : sgm.getLabel().getPods()) {
                    for (PodPort podPort : pod.getPorts()) {
                        em.persist(podPort);
                    }

                    em.persist(pod);
                }
            }

            em.persist(sgm.getLabel());
            em.persist(sgm);
        }

        sg.getSecurityGroupInterfaces().forEach(sgi -> em.persist(sgi));

        em.getTransaction().commit();
    }

    private static SecurityGroup createSecurityGroupWithPod(String baseName, int sgiCount, boolean markedForDeletion) {
        SecurityGroup sg = createSecurityGroup(baseName, 1, sgiCount, markedForDeletion);
        Label label = sg.getSecurityGroupMembers().iterator().next().getLabel();

        PodPort podPort = new PodPort(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString());

        Pod pod = new Pod(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString());

        pod.getPorts().add(podPort);
        podPort.setPod(pod);

        label.getPods().add(pod);

        return sg;
    }

    private static SecurityGroup createSecurityGroup(String baseName, int labelsCount, int sgiCount, boolean isDelete) {
        VirtualSystem vs = createVirtualSystem(baseName, MGR_TYPE);

        SecurityGroup sg = new SecurityGroup(vs.getVirtualizationConnector(), null, null);
        sg.setName(baseName + "_sg");
        sg.setMarkedForDeletion(isDelete);

        for (int i = 0; i < labelsCount; i++) {
            Label label = new Label("LABEL_NAME" + i + sg.getName(), "LABEL_VALUE" + i + sg.getName());
            SecurityGroupMember sgm = new SecurityGroupMember(sg, label);
            sg.addSecurityGroupMember(sgm);
        }

        for (int i = 0; i < sgiCount; i++) {
            SecurityGroupInterface sgi = new SecurityGroupInterface(vs, null, "1", FailurePolicyType.FAIL_CLOSE, 1L);
            sgi.setName(baseName + i + "_SGI");
            sgi.setSecurityGroup(sg);
            sg.addSecurityGroupInterface(sgi);
        }

        return sg;
    }

    private static VirtualSystem createVirtualSystem(String baseName, String mgrType) {
        VirtualizationConnector vc = new VirtualizationConnector();
        vc.setName(baseName + "_vc");
        vc.setVirtualizationType(VirtualizationType.KUBERNETES);
        vc.setVirtualizationSoftwareVersion("vcSoftwareVersion");
        vc.setProviderIpAddress(baseName + "_providerIp");
        vc.setProviderUsername("Natasha");
        vc.setProviderPassword("********");

        ApplianceManagerConnector mc = new ApplianceManagerConnector();
        mc.setIpAddress(baseName + "_mcIp");
        mc.setName(baseName + "_mc");
        mc.setServiceType("foobar");
        mc.setManagerType(mgrType.toString());

        Domain domain = new Domain(mc);
        domain.setName(baseName + "_domain");
        vc.setAdminDomainId(domain.getName());

        Appliance app = new Appliance();
        app.setManagerSoftwareVersion("fizz");
        app.setManagerType(mgrType);
        app.setModel(baseName + "_model");

        ApplianceSoftwareVersion asv = new ApplianceSoftwareVersion(app);
        asv.setApplianceSoftwareVersion("softwareVersion");
        asv.setImageUrl(baseName + "_image");
        asv.setVirtualizarionSoftwareVersion(vc.getVirtualizationSoftwareVersion());
        asv.setVirtualizationType(vc.getVirtualizationType());

        DistributedAppliance da = new DistributedAppliance(mc);
        da.setName(baseName + "_da");
        da.setApplianceVersion("foo");
        da.setAppliance(app);

        VirtualSystem vs = new VirtualSystem(da);
        vs.setApplianceSoftwareVersion(asv);
        vs.setDomain(domain);
        vs.setVirtualizationConnector(vc);
        vs.setMarkedForDeletion(false);
        vs.setName(baseName + "_vs");
        vs.setMgrId(baseName + "_mgrId");

        vc.getVirtualSystems().add(vs);
        return vs;
    }
}
