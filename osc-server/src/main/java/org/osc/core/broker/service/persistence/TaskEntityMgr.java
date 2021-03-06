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
package org.osc.core.broker.service.persistence;

import static org.osc.core.common.job.TaskState.COMPLETED;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.osc.core.broker.model.entities.job.TaskObject;
import org.osc.core.broker.model.entities.job.TaskRecord;
import org.osc.core.broker.service.dto.TaskFailureRecordDto;
import org.osc.core.broker.service.dto.TaskRecordDto;
import org.osc.core.broker.service.dto.job.LockObjectDto;
import org.osc.core.broker.service.dto.job.ObjectTypeDto;
import org.osc.core.broker.util.TransactionalBroadcastUtil;

public class TaskEntityMgr extends OSCEntityManager<TaskRecord> {

    public TaskEntityMgr(EntityManager em, TransactionalBroadcastUtil txBroadcastUtil) {
        super(TaskRecord.class, em, txBroadcastUtil);
    }

    public static TaskRecordDto fromEntity(TaskRecord tr) {
        TaskRecordDto taskDto = new TaskRecordDto();
        fromEntity(tr, taskDto);
        return taskDto;
    }

    public static void fromEntity(TaskRecord tr, TaskRecordDto taskDto) {
        taskDto.setId(tr.getId());
        taskDto.setParentId(tr.getJob().getId());
        taskDto.setName(tr.getName());
        taskDto.setStatus(tr.getStatus().name());
        taskDto.setState(tr.getState().name());
        taskDto.setQueued(tr.getQueuedTimestamp());
        taskDto.setStarted(tr.getStartedTimestamp());
        taskDto.setCompleted(tr.getCompletedTimestamp());
        taskDto.setDependencyOrder(tr.getDependencyOrder());
        taskDto.setFailReason(tr.getFailReason());
        taskDto.setPredecessors(tr.getPredecessorsOrderIds());
        taskDto.setObjects(getJobObjects(tr));

        // Set the child tasks in the Dto
        taskDto.setChildren(tr.getChildren().stream()
                .map(TaskEntityMgr::fromEntity)
                .collect(Collectors.toList()));

        // Set the Task Guard in the Dto
        taskDto.setTaskGuard(tr.getTaskGaurd().name());
        // Set the predecessor ids
        taskDto.setPredecessorIds(tr.getPredecessorsIds());
    }

    private static Set<LockObjectDto> getJobObjects(TaskRecord task) {
        if (task.getObjects() == null) {
            return null;
        }

        Set<LockObjectDto> objects = new HashSet<LockObjectDto>();
        for (TaskObject jo : task.getObjects()) {
            objects.add(new LockObjectDto(jo.getObjectId(), jo.getName(),
                    new ObjectTypeDto(jo.getObjectType().name(), jo.getObjectType().toString())));
        }
        return objects;
    }

    public List<TaskRecord> getTasksByJobId(Long jobId) {
        CriteriaBuilder cb = this.em.getCriteriaBuilder();

        CriteriaQuery<TaskRecord> query = cb.createQuery(TaskRecord.class);
        Root<TaskRecord> from = query.from(TaskRecord.class);

        query = query.select(from).where(
                cb.equal(from.join("job").get("id"), jobId))
                .orderBy(cb.asc(from.get("dependencyOrder")));

        return this.em.createQuery(query).getResultList();
    }

    /**
     * Gets all the unique Task failures encountered since the from date.
     *
     * @param fromDate the start time to look for failed task
     * @param session the hibernate session
     *
     * @return unique task failures since the from date
     */
    public static List<TaskFailureRecordDto> getUniqueTaskFailureStrings(Date fromDate, EntityManager em) {

        List<TaskFailureRecordDto> taskFailures = null;

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<TaskRecord> root = query.from(TaskRecord.class);

        Predicate restriction = null;

        if(fromDate != null) {
            restriction = cb.and(
                    cb.greaterThan(root.get("completedTimestamp"), fromDate),
                    cb.isNotNull(root.get("failReason")));
        } else {
            restriction = cb.isNotNull(root.get("failReason"));
        }

        query = query.multiselect(root.get("failReason"), cb.count(root))
                .where(restriction);

        List<?> results = em
                .createQuery(query)
                .getResultList();

        if (!results.isEmpty()) {
            taskFailures = new ArrayList<TaskFailureRecordDto>();
            for (Object row : results) {
                Object[] rowArray = (Object[]) row;
                taskFailures.add(new TaskFailureRecordDto((String) rowArray[0], (long) rowArray[1]));
            }
        }

        return taskFailures;

    }

    public List<TaskRecord> getUncompletedTasks() {
        CriteriaBuilder cb = this.em.getCriteriaBuilder();

        CriteriaQuery<TaskRecord> query = cb.createQuery(TaskRecord.class);
        Root<TaskRecord> from = query.from(TaskRecord.class);

        query = query.select(from).where(
                cb.notEqual(from.get("state"), COMPLETED));

        return this.em.createQuery(query).getResultList();
    }

}

