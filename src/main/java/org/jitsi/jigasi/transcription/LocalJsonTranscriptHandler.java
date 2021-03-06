/*
 * Jigasi, the JItsi GAteway to SIP.
 *
 * Copyright @ 2017 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.jigasi.transcription;

import net.java.sip.communicator.service.protocol.*;
import org.json.simple.*;

import java.time.*;
import java.util.*;

/**
 * This TranscriptHandler uses JSON as the underlying data structure of the
 * transcript. There are 4 kinds of JSON objects:
 *
 * 1. "final_transcript" object: contains all data regarding a conference which
 * was transcribed: start time, end time, room name, initial members, all events
 *
 * 2. "event" object: this object is used to store information regarding a
 * single transcript event. This includes speech, join/leave and raise hand
 * If it is a speech event, it includes the speech-to-text result
 * which is stored as an json-array of alternatives
 *
 * 3. "alternatives" object: This object stores one possible speech-to-text
 * result. It only has 2 fields: the text and the confidence
 *
 * 4. "Participant" object: This object stores the information of a participant:
 * the name and the (j)id
 *
 * When sending a single {@link TranscriptionResult} to the {@link ChatRoom},
 * a special JSON object is required. It needs 2 fields:
 *
 * 1. jitsi-meet-muc-msg-topic: which in our case will be a string
 *    "transcription-result"
 * 2. payload: which will be the "event" object described in point 2 above
 *
 * @author Nik Vaessen
 * @author Damian Minkov
 */
public class LocalJsonTranscriptHandler
    extends AbstractTranscriptPublisher<JSONObject>
{

    // "final transcript" JSON object fields

    /**
     * This fields stores the room name of the conference as a string
     */
    public final static String JSON_KEY_FINAL_TRANSCRIPT_ROOM_NAME
        = "room_name";

    /**
     * This field stores all the events as an JSON array
     */
    public final static String JSON_KEY_FINAL_TRANSCRIPT_EVENTS = "events";

    /**
     * This field stores "Participant" objects of the initial members as an
     * JSON array
     */
    public final static String JSON_KEY_FINAL_TRANSCRIPT_INITIAL_PARTICIPANTS
        = "initial_participants";

    /**
     * This field stores the start time of the transcript as a string
     */
    public final static String JSON_KEY_FINAL_TRANSCRIPT_START_TIME =
        "start_time";

    /**
     * This field stores the end time of the transcript as a string
     */
    public final static String JSON_KEY_FINAL_TRANSCRIPT_END_TIME =
        "end_time";


    // "event" JSON object fields

    /**
     * This fields stores the the type of event. Can be
     * {@link Transcript.TranscriptEventType#JOIN},
     * {@link Transcript.TranscriptEventType#LEAVE},
     * {@link Transcript.TranscriptEventType#RAISE_HAND} or
     * {@link Transcript.TranscriptEventType#SPEECH}
     */
    public final static String JSON_KEY_EVENT_EVENT_TYPE = "event";

    /**
     * This field stores the time the event took place as a string
     */
    public final static String JSON_KEY_EVENT_TIMESTAMP = "timestamp";

    /**
     * This field stores the  the participant who caused the event as
     * a Participant object
     */
    public final static String JSON_KEY_EVENT_PARTICIPANT = "participant";

    /**
     * This field stores the alternative JSON objects as a JSON array
     */
    public final static String JSON_KEY_EVENT_TRANSCRIPT = "transcript";

    /**
     * This field stores the language of the transcript as a string
     */
    public final static String JSON_KEY_EVENT_LANGUAGE = "language";

    /**
     * This field stores a unique id for every message as a string.
     * Can be used to update results if "is_interim" is or was true
     */
    public final static String JSON_KEY_EVENT_MESSAGE_ID = "message_id";

    /**
     * This field stores whether the speech-to-text result is an interim result,
     * which means it will be updated in the future, as either true or false
     */
    public final static String JSON_KEY_EVENT_IS_INTERIM = "is_interim";

    /**
     * This feild stores the stability value (between 0 and 1) of an interim
     * result, which indicates the likelihood that the result will change.
     */
    public final static String JSON_KEY_EVENT_STABILITY = "stability";

    // "alternative" JSON object fields

    /**
     * This field stores the text of a speech-to-text result as a string
     */
    public final static String JSON_KEY_ALTERNATIVE_TEXT = "text";

    /**
     * This fields stores the confidence of the speech-to-text result as a
     * number between 0 and 1
     */
    public final static String JSON_KEY_ALTERNATIVE_CONFIDENCE = "confidence";

    // "participant" JSON object fields

    /**
     * This fields stores the name of a participant as a string
     */
    public final static String JSON_KEY_PARTICIPANT_NAME = "name";

    /**
     * This fields stores the (j)id of a participant as a string
     */
    public final static String JSON_KEY_PARTICIPANT_ID = "id";

    /**
     * This fields stores the email of a participant as a string
     */
    public final static String JSON_KEY_PARTICIPANT_EMAIL = "email";

    /**
     * This fields stores the URL of the avatar of a participant as a string
     */
    public final static String JSON_KEY_PARTICIPANT_AVATAR_URL = "avatar_url";

    // JSON object to send to MUC

    /**
     * This fields stores the topic of the muc message as a string
     */
    public final static String JSON_KEY_TOPIC = "jitsi-meet-muc-msg-topic";

    /**
     * This field stores the value of the topic of the muc message as a string
     */
    public final static String JSON_VALUE_TOPIC = "transcription-result";

    /**
     * This field stores the payload object which will be send as a muc message
     */
    public final static String JSON_KEY_PAYLOAD = "payload";

    @Override
    public JSONFormatter getFormatter()
    {
        return new JSONFormatter();
    }

    @Override
    public void publish(ChatRoom room, TranscriptionResult result)
    {
        JSONObject eventObject = createJSONObject(result);

        JSONObject encapsulatingObject = new JSONObject();
        createEncapsulatingObject(encapsulatingObject, eventObject);

        super.sendMessage(room, encapsulatingObject);
    }

    /**
     * Create the JSON object will be send to the {@link ChatRoom}
     *
     * @param encapsulatingObject the json object which will be send
     * @param transcriptResultObject the json object which will be added as
     *                               payload
     */
    @SuppressWarnings("unchecked")
    protected void createEncapsulatingObject(JSONObject encapsulatingObject,
                                           JSONObject transcriptResultObject)
    {
        encapsulatingObject.put(JSON_KEY_TOPIC, JSON_VALUE_TOPIC);
        encapsulatingObject.put(JSON_KEY_PAYLOAD, transcriptResultObject);
    }

    /**
     * Creates a json object representing the <tt>TranscriptionResult</>.
     * @param result the object to use to produce json.
     * @return json object representing the <tt>TranscriptionResult</>.
     */
    public static JSONObject createJSONObject(TranscriptionResult result)
    {
        JSONObject eventObject = new JSONObject();
        SpeechEvent event = new SpeechEvent(Instant.now(), result);

        addEventDescriptions(eventObject, event);
        addAlternatives(eventObject, event);

        return eventObject;
    }

    @Override
    public Promise getPublishPromise()
    {
        return new JSONPublishPromise();
    }

    @Override
    protected JSONObject formatSpeechEvent(SpeechEvent e)
    {
        JSONObject object = new JSONObject();
        addEventDescriptions(object, e);
        addAlternatives(object, e);
        return object;
    }

    @Override
    protected JSONObject formatJoinEvent(TranscriptEvent e)
    {
        JSONObject object = new JSONObject();
        addEventDescriptions(object, e);
        return object;
    }

    @Override
    protected JSONObject formatLeaveEvent(TranscriptEvent e)
    {
        JSONObject object = new JSONObject();
        addEventDescriptions(object, e);
        return object;
    }

    @Override
    protected JSONObject formatRaisedHandEvent(TranscriptEvent e)
    {
        JSONObject object = new JSONObject();
        addEventDescriptions(object, e);
        return object;
    }

    /**
     * Make a given JSON object the "event" json object by adding the fields
     * eventType, timestamp, participant name and participant ID to the give
     * object
     *
     * @param jsonObject the JSON object to add the fields to
     * @param e the event which holds the information to add to the JSON object
     */
    @SuppressWarnings("unchecked")
    public static void addEventDescriptions(
        JSONObject jsonObject, TranscriptEvent e)
    {
        jsonObject.put(JSON_KEY_EVENT_EVENT_TYPE, e.getEvent().toString());
        jsonObject.put(JSON_KEY_EVENT_TIMESTAMP, e.getTimeStamp().toString());

        JSONObject participantJson = new JSONObject();
        participantJson.put(JSON_KEY_PARTICIPANT_NAME, e.getName());
        participantJson.put(JSON_KEY_PARTICIPANT_ID, e.getID());

        // adds email if it exists
        Participant participant = e.getParticipant();
        String email = participant.getEmail();
        if (email != null)
        {
            participantJson.put(JSON_KEY_PARTICIPANT_EMAIL, email);
        }

        String avatarUrl = participant.getAvatarUrl();
        if (avatarUrl != null)
        {
            participantJson.put(JSON_KEY_PARTICIPANT_AVATAR_URL, avatarUrl);
        }

        jsonObject.put(JSON_KEY_EVENT_PARTICIPANT, participantJson);
    }

    /**
     * Make a given JSON object the "event" json object by adding the fields
     * transcripts, is_interim, messageID and langiage to the given object.
     * Assumes that
     * {@link this#addEventDescriptions(JSONObject, TranscriptEvent)}
     * has been or will be called on the same given JSON object
     *
     * @param jsonObject the JSON object to add the fields to
     * @param e the event which holds the information to add to the JSON object
     */
    @SuppressWarnings("unchecked")
    private static void addAlternatives(JSONObject jsonObject, SpeechEvent e)
    {
        TranscriptionResult result = e.getResult();
        JSONArray alternativeJSONArray = new JSONArray();

        for(TranscriptionAlternative alternative : result.getAlternatives())
        {
            JSONObject alternativeJSON = new JSONObject();

            alternativeJSON.put(JSON_KEY_ALTERNATIVE_TEXT,
                alternative.getTranscription());
            alternativeJSON.put(JSON_KEY_ALTERNATIVE_CONFIDENCE,
                alternative.getConfidence());

            alternativeJSONArray.add(alternativeJSON);
        }

        jsonObject.put(JSON_KEY_EVENT_TRANSCRIPT, alternativeJSONArray);
        jsonObject.put(JSON_KEY_EVENT_LANGUAGE, result.getLanguage());
        jsonObject.put(JSON_KEY_EVENT_IS_INTERIM, result.isInterim());
        jsonObject.put(JSON_KEY_EVENT_MESSAGE_ID,
            result.getMessageID().toString());
        jsonObject.put(JSON_KEY_EVENT_STABILITY, result.getStability());
    }

    /**
     * Make a given object the "final_transcript" JSON object by adding the
     * fields roomName, startTime, endTime, initialParticipants and events to
     * the given object.
     *
     * @param jsonObject the object to add the fields to
     * @param roomName the room name
     * @param participants the initial participants
     * @param start the start time
     * @param end the end time
     * @param events a collection of "event" json objects
     */
    @SuppressWarnings("unchecked")
    private void addTranscriptDescription(JSONObject jsonObject,
                                          String roomName,
                                          Collection<Participant> participants,
                                          Instant start,
                                          Instant end,
                                          Collection<JSONObject> events)
    {
        if(roomName != null && !roomName.isEmpty())
        {
            jsonObject.put(JSON_KEY_FINAL_TRANSCRIPT_ROOM_NAME, roomName);
        }
        if(start != null)
        {
            jsonObject.put(JSON_KEY_FINAL_TRANSCRIPT_START_TIME,
                start.toString());
        }
        if(end != null)
        {
            jsonObject.put(JSON_KEY_FINAL_TRANSCRIPT_END_TIME, end.toString());
        }
        if(participants != null && !participants.isEmpty())
        {
            JSONArray participantArray = new JSONArray();

            for(Participant participant : participants)
            {
                JSONObject pJSON = new JSONObject();

                pJSON.put(JSON_KEY_PARTICIPANT_NAME, participant.getName());
                pJSON.put(JSON_KEY_PARTICIPANT_ID, participant.getId());

                // adds email if it exists
                String email = participant.getEmail();
                if (email != null)
                {
                    pJSON.put(JSON_KEY_PARTICIPANT_EMAIL, email);
                }

                String avatarUrl = participant.getAvatarUrl();
                if (avatarUrl != null)
                {
                    pJSON.put(JSON_KEY_PARTICIPANT_AVATAR_URL, avatarUrl);
                }

                participantArray.add(pJSON);
            }

            jsonObject.put(JSON_KEY_FINAL_TRANSCRIPT_INITIAL_PARTICIPANTS,
                participantArray);
        }
        if(events != null && !events.isEmpty())
        {
            JSONArray eventArray = new JSONArray();
            eventArray.addAll(events);
            jsonObject.put(JSON_KEY_FINAL_TRANSCRIPT_EVENTS, eventArray);
        }
    }

    /**
     * Formats a transcript into the "final_transcript" object
     */
    private class JSONFormatter
        extends BaseFormatter
    {
        @Override
        @SuppressWarnings("unchecked")
        public JSONObject finish()
        {
            JSONObject transcript = new JSONObject();

            addTranscriptDescription(
                transcript,
                super.roomName,
                super.initialMembers,
                super.startInstant,
                super.endInstant,
                super.getSortedEvents());

            return transcript;
        }
    }

    private class JSONPublishPromise
        extends BasePromise
    {
        /**
         * The filename wherein the Transcript will be published
         */
        private String fileName = generateHardToGuessFileName() + ".json";

        /**
         * Whether {@link this#publish(Transcript)} has already been called once
         */
        private boolean published = false;

        @Override
        public synchronized void publish(Transcript transcript)
        {
            if(!published)
            {
                published = true;

                JSONObject t
                    = transcript.getTranscript(LocalJsonTranscriptHandler.this);
                saveTranscriptToFile(getFileName(), t);
            }
        }

        @Override
        protected String getFileName()
        {
            return this.fileName;
        }
    }
}
