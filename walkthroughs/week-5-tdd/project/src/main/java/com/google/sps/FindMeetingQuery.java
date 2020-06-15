// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.*;

public final class FindMeetingQuery {
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    long duration = request.getDuration();
    ArrayList<Event> allEvents = new ArrayList();
    Set<String> attendees = new HashSet<String>(request.getAttendees());
    for (Event event : events) {
        Set<String> curAttendees = new HashSet<String>(attendees);
        curAttendees.retainAll(event.getAttendees());
        if (!(curAttendees.isEmpty())){
            allEvents.add(event);
        }
    }
    Collections.sort(allEvents);

    ArrayList<TimeRange> times = new ArrayList();
    int start = 0;
    Boolean cont = false;
    for (int i = 0; i < allEvents.size(); i++) {
        TimeRange curTime = allEvents.get(i).getWhen();
        if (!cont) {
            TimeRange curRange = TimeRange.fromStartEnd(start, curTime.start(), false);
            if ((long) curRange.duration() >= request.getDuration()) {
                times.add(curRange);
            }
            
        }
        if ((i != allEvents.size() - 1) && curTime.overlaps(allEvents.get(i+1).getWhen())) {
            cont = true;
        } else {
            start = curTime.end();
            cont = false;
        }

        if (i == allEvents.size() - 1) {
            TimeRange curRange = TimeRange.fromStartEnd(start, 1440, false);
            if ((long) curRange.duration() >= request.getDuration()) {
                times.add(curRange);
            }

        }
    }
    if (allEvents.size() == 0) {
        times.add(TimeRange.fromStartEnd(0, 1440, false));
    }
    return times;
  }
  

}
