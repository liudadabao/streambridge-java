# Capability matrix

The native adapter is derived only from the public C headers in the official
ZLMediaKit repository, pinned during implementation to commit
`9fbe034a8abf0471023ebaebf1e6132fee362dd4`.

| Domain | Public StreamBridge surface | ZLM adapter | Mock adapter |
| --- | --- | --- | --- |
| Server lifecycle | `StreamEngine` | native | in-memory |
| Stream discovery and close | `StreamEngine` | native | in-memory |
| Pull proxy | `StreamEngine.pull` | native | in-memory |
| Push proxy | `PushOperations` | native | in-memory |
| HLS/MP4 recording | `RecordingOperations` | native | in-memory |
| RTP/GB28181 receive | `RtpOperations` | native | in-memory |
| WebRTC SDP exchange | `WebRtcOperations` | native async callback | deterministic test answer |
| Media/frame input | `MediaInputOperations` | native media/frame ABI | in-memory |
| Player controls | `PlayerOperations` | native player ABI | in-memory |
| Frame delegates | `PlayerOperations.play(request, listener)` | native track/frame ABI | contract fallback |
| Stream registration events | `StreamEngine.subscribe` | native global event ABI | in-memory events |
| Runtime configuration | `RuntimeConfigurationOperations` | native option ABI | in-memory |
| HTTP/TCP/thread/transcode primitives | intentionally outside stable media API | separate low-level plugin if demanded | not applicable |

The table tracks capability domains, not a one-to-one Java copy of C symbols.
Each domain is isolated behind a small optional interface so applications can
depend only on what they use.
