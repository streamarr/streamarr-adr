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
The standard picture format that a player could use without the advanced HDR information, derived at probe time from the observed color properties and the Dolby Vision claims.
It is SDR, HDR10, HLG, none, or unspecified, and it does not promise that a device can play the source bytes unchanged.
_Avoid_: base layer (a Dolby Vision layer, not a picture format), fallback, SDR fallback

**Codec profile**:
The profile and level that a video codec such as H.264, HEVC or AV1 signals for a video stream.
_Avoid_: profile (unqualified), viewing profile (the household concept in ADR 0024)

**Dolby Vision profile**:
The profile, level and base-layer compatibility that Dolby Vision signalling declares for a video stream, kept separate from the codec profile.
_Avoid_: profile (unqualified), DV profile

### Playback records

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
