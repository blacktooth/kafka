/**
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License.  You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package kafka.utils.timer

import org.apache.kafka.server.util.MockTime
import org.apache.kafka.server.util.timer.{Timer, TimerTask, TimerTaskEntry}

import scala.collection.mutable

class MockTimer(val time: MockTime = new MockTime) extends Timer {

  private val taskQueue = mutable.PriorityQueue.empty[TimerTaskEntry](new Ordering[TimerTaskEntry] {
    override def compare(x: TimerTaskEntry, y: TimerTaskEntry): Int = java.lang.Long.compare(x.expirationMs, y.expirationMs)
  }.reverse)

  def add(timerTask: TimerTask): Unit = {
    if (timerTask.delayMs <= 0)
      timerTask.run()
    else {
      taskQueue synchronized {
        taskQueue.enqueue(new TimerTaskEntry(timerTask, timerTask.delayMs + time.milliseconds))
      }
    }
  }

  def advanceClock(timeoutMs: Long): Boolean = {
    time.sleep(timeoutMs)

    var executed = false
    val now = time.milliseconds

    var hasMore = true
    while (hasMore) {
      hasMore = false
      val head = taskQueue synchronized {
        if (taskQueue.nonEmpty && now > taskQueue.head.expirationMs) {
          val entry = Some(taskQueue.dequeue())
          hasMore = taskQueue.nonEmpty
          entry
        } else
          None
      }
      head.foreach { taskEntry =>
        if (!taskEntry.cancelled) {
          val task = taskEntry.timerTask
          task.run()
          executed = true
        }
      }
    }
    executed
  }

  def size: Int = taskQueue.size

  override def close(): Unit = {}

}
