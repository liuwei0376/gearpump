/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gearpump.cluster.scheduler

import akka.actor.{ActorRef, Actor}
import org.apache.gearpump.TimeStamp
import org.apache.gearpump.cluster.Master.WorkerTerminated
import org.apache.gearpump.cluster.MasterToWorker.{UpdateResourceFailed, WorkerRegistered}
import org.apache.gearpump.cluster.WorkerInfo
import org.apache.gearpump.cluster.WorkerToMaster.ResourceUpdate
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

abstract class Scheduler extends Actor{
  private val LOG: Logger = LoggerFactory.getLogger(classOf[Scheduler])
  protected var resources = new mutable.HashMap[WorkerInfo, Resource]

  def handleScheduleMessage : Receive = {
    case WorkerRegistered(id) =>
      val workerInfo = WorkerInfo(id, sender())
      if(!resources.contains(workerInfo)) {
        LOG.info(s"Worker $id added to the scheduler")
        resources.put(workerInfo, Resource.empty)
      }
    case ResourceUpdate(worker, resource) =>
      LOG.info(s"Resource update id: ${worker.id}, slots: ${resource.slots}....")
      if(resources.contains(worker)) {
        resources.update(worker, resource)
        allocateResource()
      }
      else {
        worker.actorRef ! UpdateResourceFailed(s"ResourceUpdate failed! The worker ${worker.id} has not been registered into master")
      }
    case WorkerTerminated(actor) =>
      resources = resources.filter( params => {
        val (workerInfo, _) = params
        workerInfo.actorRef != actor
      })
  }

  def allocateResource(): Unit
}

object Scheduler{
  class PendingRequest(val appMaster: ActorRef, val request: ResourceRequest, val timeStamp: TimeStamp)
}