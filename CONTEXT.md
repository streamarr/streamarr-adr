# Streamarr Domain Language

This glossary defines shared Streamarr domain terms.

## Language

### Probe

**Probe properties**:
The technical facts about a media file's container and streams that a probe reports; stream properties are the facts about one stream.
_Avoid_: metadata, technical metadata

**Metadata**:
Descriptive data about a title, such as the titles, artwork and credits that TMDB supplies.
_Avoid_: metadata for technical facts, probe metadata

**MediaFileProbePropertyIssue**:
A problem found when checking the container or stream properties reported by a media-file probe.
A probe can succeed while the reported properties contain problems.
_Avoid_: VideoPropertyIssue, MediaPropertyIssue, MediaFilePropertyIssue

### Video properties

**Video stream**:
One video track of a media file as the probe describes it.
_Avoid_: track, stream (unqualified), stream session (the runtime playback concept)

**Dynamic range**:
The structured description of a video stream's HDR features: its compatible base, whether HDR10+ is present, and its Dolby Vision details.
Each feature is present, absent, or unspecified when the probe could not check, and the broad HDR or SDR label is derived from the description and never stored.
_Avoid_: HDR flag, HDR type, HDR format, HDR boolean

**Compatible base**:
The standard picture format that a player could use without the advanced HDR information, derived at probe time from the base layer's signalled transfer and the Dolby Vision claim.
It is SDR, PQ, HLG, none, or unspecified, and it does not promise that a device can play the source bytes unchanged.
_Avoid_: base layer (a Dolby Vision layer, not a picture format), HDR10 (PQ with static metadata, a narrower fact), fallback, SDR fallback

**Codec profile**:
The profile and level that a video codec such as H.264, HEVC or AV1 signals for a video stream.
_Avoid_: profile (unqualified), viewing profile (the household concept in ADR 0024)

**Dolby Vision profile**:
The profile, level and base-layer compatibility that the Dolby Vision configuration record declares for a video stream, kept separate from the codec profile.
Its values are claims that the probe checks against the picture signalling and the samples.
_Avoid_: profile (unqualified), DV profile

### Playback records

**Playback session**:
The logical span of one viewing of a title, which survives replacement stream sessions and holds the format-failure history and the format-attempt budget.
_Avoid_: playback (ADR 0018's token and request surface), app session, stream session (one server-side run inside it)

**Source description**:
The record of one media track as the probe found it.
It never changes when the server chooses a playback output.
_Avoid_: source metadata, input description

**Client capability declaration**:
The record of the combinations that a player and its delivery path can accept, including any display constraints.
The server chooses an output from it, and playback can still fail.
_Avoid_: device profile, client profile, capabilities list

**Planned output**:
The output that the server chooses from the source description, the client capability declaration, the verified worker capabilities and the user's playback choice.
_Avoid_: target format, requested output

**Produced output**:
The media that the worker actually produces and that the playlist must describe.
Under fragmented MP4 delivery, the initialization segment describes it on its own.
_Avoid_: actual output, transcoded output

### Segment delivery

**Producer**:
The worker module that owns one job attempt's FFmpeg process, reads its standard output, and delivers that attempt's initialization segment and media segments.
_Avoid_: engine, transcoder, pipeline

**Fragment**:
One movie-fragment pair (`moof` and `mdat`) that FFmpeg emits, and the smallest unit the producer reads. A fragment starts at a keyframe or when the fragmentation target elapses.
_Avoid_: chunk, part

**Media segment**:
The unit an HLS playlist advertises, always fragmented MP4. Segment N is the fragments whose first video sample is a keyframe inside media time [N × period, (N + 1) × period), together with the fragments that follow them before the next such keyframe.
_Avoid_: chunk, segment file, .ts, container format

**Initialization segment**:
The `ftyp` and `moov` boxes a player needs before any media segment of a variant. There is one per variant, and it is identical across every attempt that the same encoder backend produces.
_Avoid_: init file, header, init.mp4

**Media time**:
Presentation time measured from the source container's start time, on the zero-based timeline that every attempt of a stream session shares.
_Avoid_: PTS, source time, wall-clock time

**Fragmentation target**:
The configured maximum media duration of a fragment. The muxer honours it at the next packet, so it is a target, never a hard bound.
_Avoid_: fragment bound, chunk size

**Preroll**:
Media before a job attempt's first media segment that its seek emits: a stream copy's seek lands on the keyframe at or before the target, and an attempt that encodes video and starts after segment 0 seeks one period early on purpose. It belongs to earlier segments, so the producer discards it.
_Avoid_: overlap, lead-in

**Stream copy**:
Any transcode mode that passes the source video through unchanged (REMUX and AUDIO_TRANSCODE). Its segment boundaries depend on the source's own keyframes.
_Avoid_: passthrough, remux (when only the video is copied)

**Keyframe-verified encoder**:
An encoder whose recordings under the pinned FFmpeg show every forced keyframe as a sync sample that starts a closed GOP, with the encoder's GOP count restarting there. It says nothing about whether a worker can run the encoder, which the verified worker capabilities describe.
_Avoid_: verified encoder (unqualified), GOP-verified encoder

**Source container**:
The container of a media file as the probe found it, such as Matroska, MP4 or MPEG-TS. It is a source fact that decides direct-play eligibility, and it never describes HLS delivery, which is always fragmented MP4.
_Avoid_: container format, ContainerFormat, output container

### Attempts

**Job attempt**:
One dispatch of a variant's job to a worker, with its own FFmpeg process, producer and outcome: completed, failed or stopped.
_Avoid_: dispatch attempt, run, session

**Replacement attempt**:
A job attempt that replaces an earlier one for the same variant under ADR 0019's recovery. It keeps the variant's initialization segment, media time and encoder backend.
_Avoid_: retry, restart, replacement (unqualified)

**Format attempt**:
One output format that the server chooses within a playback session, delivered by its own stream session and initialization segment. The initial format is the first attempt, and each Auto recovery after a format error adds one to the playback session's budget.
_Avoid_: fallback stream, replacement (unqualified)

**Encoder backend**:
The encoder a worker actually runs for a codec family, together with whether it is hardware or software. A variant's first job attempt pins the backend for every replacement attempt.
_Avoid_: encoder capability, codec

**Completed attempt**:
A job attempt whose FFmpeg exited cleanly after the producer read all of its output and the server acknowledged every delivered segment, while the attempt was still active. A completed attempt does not mean the run covered the advertised timeline; the server owns coverage.
_Avoid_: finished, done, success
