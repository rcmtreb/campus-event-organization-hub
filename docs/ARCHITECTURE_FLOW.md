# System Architecture & Logic Flow

## 1. Attendance Logic Flow
[cite_start]The most critical technical hurdle is the **Single Redemption Code** system[cite: 21].

1.  **Generation**: Officer generates a unique code for the event session.
2.  **Input**: Student enters code in the mobile UI.
3.  **Validation**: 
    * [cite_start]System checks if the Student's "Tag" matches the Event's required "Tag"[cite: 20].
    * System checks if "Time In" has already been recorded.
4.  **Action**: Record timestamp and update status to "Present."

## 2. Notification Pipeline
* [cite_start]**Trigger**: Officer publishes an event[cite: 7, 18].
* [cite_start]**Filter**: Logic checks student profiles for matching `course` or `department` tags[cite: 20].
* [cite_start]**Delivery**: Push notification sent to the `student_id` associated with those tags[cite: 6, 19, 20].

## 3. Data Flow Diagram (High Level)
[Mobile App] <--> [Backend API / Firebase] <--> [Database]
      ^                                           |
      |                                           v
[Push Notification Service] <---------------- [Admin Dashboard]