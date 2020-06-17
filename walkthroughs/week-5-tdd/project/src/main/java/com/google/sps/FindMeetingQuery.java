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
import java.lang.Math;

public final class FindMeetingQuery {
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    long duration = request.getDuration();
    ArrayList<Event> allEvents = new ArrayList();
    ArrayList<Event> allEventsOptional = new ArrayList();
    Set<String> attendees = new HashSet<String>(request.getAttendees());
    Set<String> allAttendeesOptional = new HashSet<String>(request.getAttendees());
    allAttendeesOptional.addAll(request.getOptionalAttendees());
    //Add all events that attendees go to
    for (Event event : events) {
        Set<String> curAttendees = new HashSet<String>(attendees);
        Set<String> curAttendeesOptional = new HashSet<String>(allAttendeesOptional);
        curAttendees.retainAll(event.getAttendees());
        curAttendeesOptional.retainAll(event.getAttendees());
        if (!(curAttendees.isEmpty())){
            allEvents.add(event);    
        } 
        if(!(curAttendeesOptional.isEmpty())){
            allEventsOptional.add(event);
        }
        
    }
    Collections.sort(allEvents);
    Collections.sort(allEventsOptional);
    ArrayList<TimeRange> timesOptional = getTimes(allEventsOptional, request);
    if (!(timesOptional.isEmpty()) || request.getAttendees().size() == 0){
        return timesOptional;
    }
    ArrayList<TimeRange> times = getTimes(allEvents, request);
    return times;
  }

  private ArrayList<TimeRange> getTimes(ArrayList<Event> allEvents, MeetingRequest request) {
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
        //If next event overlaps with current, make start the greater of the two events
        if ((i != allEvents.size() - 1) && curTime.overlaps(allEvents.get(i+1).getWhen())) {
            start = Math.max(curTime.end(), start);
            cont = true;
        } else {
            //If it doesn't overlap check gap during next iteration
            start = Math.max(curTime.end(), start);
            cont = false;
        }
        //If last event check if end of day is a possible time
        if (i == allEvents.size() - 1) {
            TimeRange curRange = TimeRange.fromStartEnd(start, TimeRange.END_OF_DAY, true);
            if ((long) curRange.duration() >= request.getDuration()) {
                times.add(curRange);
            }
        }
    }
    if (allEvents.size() == 0 && request.getDuration() <= TimeRange.END_OF_DAY) {
        times.add(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TimeRange.END_OF_DAY, true));
    }
    return times;
  }
}
