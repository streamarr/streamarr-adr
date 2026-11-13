# Streamarr Domain Language

This glossary defines shared Streamarr domain terms.

## Language

### Identity

**Account**:
The login and security principal.
An Account authenticates and makes every request, and a Profile never does.
Every Account is a member of exactly one Household and has exactly one Personal Profile.
_Avoid_: user (unqualified), Profile (the viewing identity that an Account selects)

**Household**:
A private boundary for administration, catalog and local policy.
Every Account is a member of exactly one Household, and every Profile belongs to exactly one Household.
An Account may also use another Household while its Personal Profile is shared there.
_Avoid_: home Household (ADR 0022's wording)

**Profile**:
A portable viewing identity with its own name, picture, PIN, Kid or Adult kind, optional Content Ceiling, viewing history, progress and preferences.
A Profile belongs to exactly one Household and can be shared into others, and its data and policy go with it.
A Profile cannot sign in or hold administrative authority.
_Avoid_: viewing profile, profile (unqualified, where a codec profile could be meant)

**Personal Profile**:
The one Profile that represents an Account.
Every Account has exactly one Personal Profile, and a Profile represents at most one Account.
The Account and its Personal Profile transfer together, and the Account cannot unlink its Personal Profile.

**Selected Profile**:
The Profile that a session acts for.
The Account picks the selected Profile in the Profile picker, and the Profile-scoped access token names it.
A request has at most one selected Profile, and no argument of a request names the selected Profile.
_Avoid_: current Profile, active Profile, viewer (the person who watches)

**HouseholdAdmin**:
The Household role that edits a Household's settings and manages its Devices.
Every Household that has Accounts keeps at least one HouseholdAdmin.
HouseholdAdmins are peers, and every HouseholdAdmin is also a HouseholdMember.
While a Profile is shared into the Household, its HouseholdAdmins administer that Profile there and supervise it if it is restricted.
_Avoid_: owner, HouseholdOwner, parent (household roles that ADR 0024 removed)

**ProfileManager**:
An Account with durable authority to edit a Profile and its policy, manage its PIN, offer it to Households and invite other managers.
The authority comes from a direct grant, or it derives from the Account's own unrestricted Adult Personal Profile.
ProfileManager authority survives the end of a share, unlike a HouseholdAdmin's authority over a shared Profile.
_Avoid_: owner, parent

**ServerAdmin**:
Global superuser authority that an Account may hold beside its Household role.
ServerAdmin authority is not Household membership.
Every ServerAdmin operation checks live authority in PostgreSQL rather than trusting a token claim.
At least one enabled ServerAdmin must always remain.
_Avoid_: admin (unqualified), `ADMIN` account role (ADR 0015's superseded role)

**Device registration**:
The durable record that fixes a shared Device, such as a television, to one Household.
A Device registration records the Device name, its Electronic Serial Number (ESN), the Household and the Account that linked the Device.
Only the winning poll of ADR 0021's pairing flow creates a Device registration.
A Device registration never expires, and only revocation ends it.
_Avoid_: pairing grant (the short-lived approval record that comes before it)

**Share code**:
A short code that a HouseholdAdmin, or a ServerAdmin, issues for one Household so that whoever holds it can offer a Profile to that Household.
Looking it up names the Household and tells nothing else about it.
A share code admits one offer and is then spent.
It also stops working when it expires, when a HouseholdAdmin withdraws it, or when the Account that issued it loses the authority to issue it or is disabled.
A Household can hold several at once.
Issuing one is an invitation to offer, never consent: every offer still waits for a HouseholdAdmin to accept or reject it (ADR 0024).
_Avoid_: Household id (the key that a share code replaces), invite code or invitation code (the code of an Account invitation or a ProfileManager invitation), join code (a share code makes nobody a member), user code or linking code (the code that links a Device), share link (a client may carry the code in a link, but the code is the term)

### Probe

**Probe properties**:
The technical facts about a media file's container and streams that a probe reports. Stream properties are the facts about one stream.
_Avoid_: metadata, technical metadata

**Metadata**:
Descriptive data about a media title, person or company, such as names, artwork and credits.
_Avoid_: metadata for technical facts, probe metadata

**Probe report**:
Everything that a worker sends back for one probe attempt.
A probe report always holds the result.
A probe attempt that the server canceled has no probe report.
A probe report holds the keyframe positions only when the probe found the keyframe positions and their number is within the server's limit.
The same rule applies to the measurement.
The worker sends each probe report in a call of its own, never on the worker session (ADR 0062).
_Avoid_: probe reply, probe response, probe result (the result is one part of the probe report)

**MediaFileProbePropertyIssue**:
A problem found when checking the container or stream properties reported by a media-file probe.
A probe can succeed while the reported properties contain problems.
The server derives each issue from the stored probe properties when it decides, and never stores an issue.
_Avoid_: VideoPropertyIssue, MediaPropertyIssue, MediaFilePropertyIssue

### Video properties

**Video stream**:
One video track of a media file as the probe describes it.
_Avoid_: track, stream (unqualified), stream session (the runtime playback concept)

**Chosen video stream**:
The video stream of a media file that the transcode decision reads and FFmpeg maps.
The chosen video stream is the first video stream that is not an attached picture, such as embedded cover art.
_Avoid_: first video stream, `0:v:0`, main video stream

**Dynamic range**:
The structured description of a video stream's HDR features: its compatible base, whether HDR10+ is present, and its Dolby Vision details.
Each feature is present, absent, or unspecified when the probe could not check, and the broad HDR or SDR label is derived from the description and never stored.
_Avoid_: HDR flag, HDR type, HDR format, HDR boolean

**Compatible base**:
The standard picture format that a player could use without the advanced HDR information, derived at probe time from the base layer's signaled transfer and the Dolby Vision claim.
It is SDR, PQ, HLG, none, or unspecified, and it does not promise that a device can play the source bytes unchanged.
_Avoid_: base layer (a Dolby Vision layer, not a picture format), HDR10 (PQ with static metadata, a narrower fact), fallback, SDR fallback

**Codec family**:
The video coding standard of a video stream, such as H.264, HEVC or AV1, without the stream's codec profile, level or codec string.
The probe reads a video stream's codec family from the codec name that ffprobe reports for the stream.
The server reads the same codec family from the codec name that the probe stored.
In code, `ProbedCodecFamily` holds the codec family that the probe read: `H264`, `HEVC`, `AV1` or `OTHER`.
`OTHER` covers every remaining codec family, such as VP9, because Streamarr has no rules for those codec families.
An encode targets one codec family, which the worker contract carries as a `VideoCodecFamily` value.
Every transcode job also carries the source's codec family as a `VideoCodecFamily` value.
The job's source codec family is unspecified for an `OTHER` source.
_Avoid_: codec (unqualified), video codec, format (a container format or an output format)

**Codec profile**:
The profile and level that a video stream signals within the stream's codec family, such as H.264, HEVC or AV1.
_Avoid_: profile (unqualified), Profile (the portable viewing identity in ADR 0024)

**Codec string**:
The RFC 6381 value that names one stream's codec with its codec profile, level and other parameters, such as `avc1.640028`.
A variant's `CODECS` attribute lists the codec strings of the variant's streams.
An encode's codec string describes its promised output, including the codec profile, level, tier, bit depth, compatibility flags and constraints that the output signals.
The probe computes the codec string of every video and audio stream.
A stream copy uses the source stream's codec string, with the sample-entry tag that ADR 0013's tag rule picks for the copy.
For example, a Dolby Vision copy carries its base layer's tag, so a `dvh1` source stream's copy is described as `hvc1`.
Each codec string must match the produced initialization segment.
_Avoid_: codec (the codec family alone), codec tag (the sample-entry four-character code), CODECS string (the attribute can list several)

**Dolby Vision profile**:
The profile, level and base-layer compatibility that the Dolby Vision configuration record declares for a video stream, kept separate from the codec profile.
Its values are claims that the probe checks against the picture signaling and the samples.
When no configuration record names a profile, the probe derives the profile from the first RPU header, as FFmpeg does.
_Avoid_: profile (unqualified), DV profile

**MEL**:
A minimal enhancement layer: the second HEVC layer of a Dolby Vision profile 7 stream when that layer adds no picture detail to the base layer.
An RPU describes a MEL when its NLQ parameters, the parameters that tell a dual-layer decoder how to add the residual, hold an offset of 0, a `vdr_in_max` of exactly 1, a dead-zone slope of 0 and a dead-zone threshold of 0 for every color component.
A stream is MEL when an RPU describes a MEL and no RPU describes a FEL.
A copy converts a single-track MEL stream to profile 8.1, and each RPU that describes a MEL keeps its mapping curves (ADR 0036).
_Avoid_: empty enhancement layer, dummy layer, EL (unqualified), enhancement layer (unqualified) for the type

**FEL**:
A full enhancement layer: the second HEVC layer of a Dolby Vision profile 7 stream when that layer carries residual picture detail that a dual-layer decoder adds to the base layer.
An RPU describes a FEL when its NLQ parameters are anything other than a MEL's, or when it uses floating-point coefficients.
A stream is FEL when any RPU describes a FEL.
A copy converts a single-track FEL stream to profile 8.1 (ADR 0036).
Each RPU that describes a FEL gets identity mapping curves in place of its own, and each RPU that describes a MEL keeps its curves, whatever the stream's type.
The converted copy loses the residual picture detail, because Streamarr does not compose a FEL on a device.
_Avoid_: full layer, complete enhancement layer, lossless profile 7, EL (unqualified), enhancement layer (unqualified) for the type

**RPU placement**:
Where the samples of an H.264 or HEVC Dolby Vision video stream put each picture's RPU, the unit that holds the picture's Dolby Vision reshaping instructions.
The probe reports it from the first packets: each RPU follows its own picture, every RPU starts its sample, or unspecified when the probe cannot tell.
Every RPU starts its sample in two layouts: the shifted layout, where each picture's RPU starts the next picture's sample, and the own-first layout, where it starts its own picture's sample.
One value covers both, because the first packets of a shifted file whose start was cut away look like an own-first file's, and both take the same repair.
The placement rules out a copy when every RPU starts its sample, and never when it is unspecified.
The worker reads the placement before an encode that must reshape the pictures, to move each RPU behind the picture that it belongs to, under the conditions that ADR 0013 sets.
_Avoid_: RPU order, RPU position, RPU offset, RPU layout for the reported value (a layout is the arrangement in a file that the placement reports)

**Output frame rate**:
The frame rate that the frame-rate rule chooses for a file's chosen video stream.
The probe job chooses the output frame rate once per file, and the probe outcome stores the output frame rate.
Every encode of a progressive source runs at the output frame rate.
An encode that writes one frame per field of an interlaced source runs at twice the output frame rate.
The transcode job carries the rate at which the encode runs, and the worker's FFmpeg argument, GOP size, forced keyframes and sample-duration check all use the job's rate.
The probe outcome, the transcode job and the worker's FFmpeg argument carry each rate as an exact fraction, never as a decimal.
Every job attempt of a stream session therefore encodes on one frame grid, which is the same sequence of output frame times.
_Avoid_: target frame rate, frame rate (unqualified) for the encode's rate

### Playback records

**Playback session**:
The logical span of one viewing of a media title, which survives replacement stream sessions and holds the format-failure history and the format-attempt budget.
_Avoid_: playback (ADR 0018's token and request surface), app session, stream session (one server-side run inside it)

**Decision reason**:
A typed value that the stream decision records for why a stream session plays the file directly, copies a stream or encodes it, such as a codec that the client does not declare. The playback session keeps the reasons. A viewer sees them in the playback-data panel for that viewer's own playback, and a ServerAdmin sees every active stream session's reasons in the active-streams view (ADR 0013).
_Avoid_: transcode reason (it also explains copies and direct play), log message

**Format change**:
The difference between what plays and what the viewer's playback choice asked for, which the server names in the playback session while playback continues. The server names six: `COPY_FAILED`, `DIRECT_PLAY_FAILED`, `STARTED_WITH_ENCODE`, `DOLBY_VISION_STRIPPED`, `AUDIO_TRACK_SWITCHED`, and `HDR_NOT_KEPT` for SDR that plays where HDR was chosen or played, or where the upload budget turned an HDR copy into an encode in Auto. A playback session names one format change at a time and keeps it until the playback session ends. When the viewer picked Auto, the server names only `HDR_NOT_KEPT`. The client shows a format change as the caution symbol on its quality control (ADR 0052).
_Avoid_: fallback (unqualified), notice, warning, error (playback has not stopped)

**Caution symbol**:
The alert triangle that a client shows on the corner of a player control while playback continues but differs from what the viewer chose. The picker that the control opens carries the caution symbol in its heading, and that caution symbol shows a message that explains the difference. The caution symbol shows only with the player controls. The quality control carries it for a format change, and the audio control carries it after the server refuses an audio switch during direct play. On the quality control, the caution symbol goes when the viewer picks a different playback choice that plays as picked. A pick of the playback choice that the quality control already shows as selected clears nothing. On the audio control, the caution symbol goes when the viewer picks any audio track, the audio track that plays included. Every caution symbol also goes when the viewer leaves the media title (ADR 0052).
_Avoid_: notice (the overlay that it replaced), warning icon, alert (the failed state of playback that stopped)

**Audio delay**:
A signed offset, from −2,000 ms to +2,000 ms in 10 ms steps, that a viewer sets to correct the lip-sync error of one device's playback chain. A positive delay plays the sound later than the picture. The client keeps it per device and per audio output and sends it with the playback session, which keeps it while it lasts (ADR 0055).
_Avoid_: audio offset, lip-sync offset, A/V sync setting, audio latency (what the chain adds, not the correction), applied audio delay (the sum that the server shifts by)

**Audio sync correction**:
A signed offset, in the audio delay's range and step, that a ServerAdmin sets for a media file whose own audio is out of sync. One value covers all the file's audio streams, and an audio stream can carry its own, which replaces the file's for that stream. The server stores it with the media file, applies it for every viewer and device, and removes it when the source snapshot changes (ADR 0055).
_Avoid_: title offset, audio offset, audio delay (the device's value), metadata edit (a catalog entry's field, which survives a file upgrade), override (reserved for administrative overrides in ADR 0024)

**Applied audio delay**:
The amount by which the server shifts one audio rendition: the playback session's audio delay plus the rendition's audio sync correction, clamped to −2,000 ms to +2,000 ms. The server moves decode times and never changes a sample (ADR 0055).
_Avoid_: total delay, effective delay (effective value is a metadata term), audio delay (the device's part alone)

**Playback report**:
A snapshot that a viewer shares with the Server Admin from the playback-data panel: the server's decisions for the viewer's own playback session, the player's figures at the moment the viewer shared it, and an optional note of up to 280 characters. A ServerAdmin reads it in the Shared reports list and marks it read. The server deletes it 30 days after it was shared (ADR 0013).
_Avoid_: bug report, issue, ticket, feedback, diagnostics upload (no file is uploaded)

**Source description**:
The record of one media track as the probe found it.
It never changes when the server chooses a playback output.
_Avoid_: source metadata, input description

**Client capability declaration**:
The record of the combinations that a player and its delivery path can accept, including any display constraints.
A client capability declaration lists capability entries.
The server chooses an output from it, and playback can still fail.
_Avoid_: device profile, client profile, capabilities list

**Capability entry**:
One group of values and maximums that the player accepts together for one video codec family or one audio codec.
An output fits the declaration only when one capability entry accepts every property of the output.
Its maximum picture width and height bound a picture in either orientation (ADR 0049).
_Avoid_: codec profile (a profile of the coding standard), device profile, condition

**Viewer's limits**:
The limits that bound a stream session's output apart from the client capability declaration: the picture box of the fixed quality that the viewer picked, and the maximum picture width, maximum picture height and maximum bitrate that the client sends.
They bound every encode. An encode that Original reaches plays in Auto (ADR 0052).
Outside Original, they also bound copies, except a copy of a source that cannot be encoded or that the server has nothing to encode to.
The server has nothing to encode to when the client capability declaration rejects every encode of the source, or when workers are connected but none has a worker-verified encoder for a codec that the declaration accepts an encode in.
The maximum width and height apply as the client gives them, and the maximum bitrate covers video plus audio (ADR 0050).
When the audio leaves no room for the 360p tier under the maximum bitrate, the server lowers the audio through the audio steps before the stream exceeds the maximum.
Outside Original, a video copy costs its video plus the session's audio, and the server lowers the audio before it encodes the video.
_Avoid_: client capabilities (what the player can decode), quality cap

**Network route**:
How the client reached the server for a playback session, as the client attests it: `LOCAL_NETWORK` or `INTERNET`. The client sends `LOCAL_NETWORK` for a private, link-local or unique local address, a `.local` or `.home.arpa` name, or local discovery, and `INTERNET` for every other route, loopback, Tailscale and single-label host names included. Every format attempt of the playback session keeps it. The server never classifies a viewer by IP address. The route decides only whether the stream session counts against the upload budget, and it grants no access (ADR 0050).
_Avoid_: home, away, at home (ADR 0050's earlier wording), network class, location

**Upload budget**:
The optional share of the server's internet upload that counted stream sessions may use together: 80% of the upload speed that a ServerAdmin enters, off until one is entered. A stream session counts at the average bitrate of the variant that its player last requested, unless its network route is `LOCAL_NETWORK` or it is suspended. When the server serves a multivariant playlist, the budget leaves out the variants that would take the counted stream sessions over the share and always keeps the lowest. It never refuses playback and never changes a direct play. It turns a copy into an encode in one case only. In Auto, a new stream session's copy that the playlist lists alone and that does not fit the share plays as an encode instead, whatever the copy's range. When that encode is SDR and the copy would have played HDR, the server names the format change `HDR_NOT_KEPT` (ADR 0052). Viewer messages call the upload budget "the server's configured upload limit" (ADR 0052). The copy still plays over the share when no encode can start. A copy in Original always plays (ADR 0050). A stream session that replaces another after a lower variant beside the copy fails follows the same rule, and its encode leaves the failed lower variant's encode out (ADR 0013).
_Avoid_: bandwidth limit, bandwidth cap, upload limit (in code and docs, where it names the worker's segment upload limit), quota, throttle

**Language override**:
A Profile's choice of audio language, subtitle language or subtitle mode for one episode or movie, one season or one series. An empty choice comes from the next wider level. The choice that the client sends when it starts a stream session comes first, then the most specific override, then the Profile's preferences, then the file's own default. A switch of audio or subtitles in the player becomes the Profile's override for that episode or movie only, and never carries to the next episode; turning on a full subtitle stores its language with the mode `ALWAYS`. A change on a settings screen outside the player stores the override of that screen's episode, movie, season or series (ADR 0043).
_Avoid_: track preference, per-title track, per-track override, default track (the file's flag)

**Preferred audio languages**:
A Profile's ordered list of at most five audio languages. A stream session plays the first language in the list that the file holds in a track that can be the default. Each language is a value of the server's language table, which the schema exposes as the `Language` enum and which maps each value to the codes that files carry (ADR 0043).
_Avoid_: preferred language (unqualified), language code (for the enum value)

**Subtitle mode**:
A Profile's rule for when a subtitle track starts on: `OFF` starts none, not even a forced track; `FORCED`, the default, starts only a forced track in the selected audio's language; `ALWAYS` starts the track in the Profile's default subtitle language (ADR 0043).
_Avoid_: subtitle preference, subtitles on or off (for the stored setting), auto subtitles, foreign audio (no such mode)

**Audio step**:
One audio codec, channel layout and bitrate that the server can deliver in place of the stream session's audio under the viewer's maximum bitrate: the session's own audio first, then a lower-rate step for a FLAC or ALAC rendition above 48 kHz (ADR 0043), then multichannel steps at lower bitrates, then AAC stereo at 128 and 96 kb/s, or AAC mono at 96 kb/s for a mono track or when no AAC entry for HLS accepts more than one channel.
Multichannel steps keep the codec and channel layout of the step track's encode, or of the E-AC-3, AC-3 or AAC encode that the server would choose when that encode is FLAC, ALAC or Opus.
A copied track in a codec with multichannel steps of its own (AAC, AC-3 or E-AC-3) adds steps in its own codec, up to 5.1; a copied AAC track above 5.1 steps through AAC 5.1 at 320 kb/s, or AAC 5.0 for a `7.0` track, because an AAC encode has at most six channels.
A step keeps a track's 3 or 4 channels only in a layout that the step's codec carries exactly: `3.0` or `4.0` in AAC, and `2.1`, `3.0`, `4.0` or the one 2/2 mode of `quad` and `quad(side)` in AC-3 and E-AC-3; any other or unknown layout steps to stereo.
All of these steps run from the highest bitrate to the lowest.
The server steps down to fit the 360p tier or a video copy, and takes a lower multichannel step when it lets a larger picture fit.
The server takes one step for the whole stream session, from the largest audio rendition, and a step below the session's audio caps every audio rendition (ADR 0050).
_Avoid_: audio tier, audio quality, audio variant (a rendition is never a variant)

**Downmix**:
An audio encode whose output layout lacks a channel of the source track's layout, so that it mixes source channels together with ADR 0050's named matrix or leaves one out.
A side pair and a back pair are one surround pair, so an encode that only renames that pair is not a downmix and writes every sample unchanged.
An encode that pads a layout with silent channels, such as a `7.0` track to Opus or FLAC `7.1`, keeps every channel in place, so it is not a downmix either.
A downmix that only leaves channels out has no limiter; otherwise the limiter acts only on the channels that the matrix sums, in two stages, at −1 dBTP when FLAC or ALAC writes the downmix and −3 dBTP for a lossy codec, as the BS.1770 Annex 2 filter reads them (ADR 0050).
_Avoid_: downmix for an encode that only renames a surround pair or pads with silent channels; remix; fold-down

**Quality tier**:
A named 16:9 picture box that the quality ladder offers: 360p, 480p, 720p, 1080p, 1440p or 2160p.
The box bounds a picture in either orientation, so the 1080p box holds 1920×1080 and 1080×1920.
A tier's variant is the source scaled to fit the box, never upscaled, cropped, padded or stretched.
In Auto, the variant takes the tier's name as its label only when its picture reaches the box: it fits, and the worker's next larger picture of the source would not. Otherwise its label is its shorter side, such as 900p.
Each tier has a nominal bitrate for each codec family.
Each fixed quality has the box of one quality tier. Its single variant has no label, and it encodes at the bitrate of the smallest tier whose box holds the encoded picture, capped by the source's bitrate and the viewer's maximum bitrate (ADR 0050).
_Avoid_: resolution, height (as a tier's key), preset (an encoder speed setting)

**Planned output**:
The output that the server chooses from the source description, the client capability declaration, the verified worker capabilities and the user's playback choice.
_Avoid_: target format, requested output

**Produced output**:
The media that the worker actually produces and that the playlist must describe.
Under fragmented MP4 delivery, the initialization segment describes it on its own.
_Avoid_: actual output, transcoded output

**Bitstream rewrite**:
A change that the worker makes to a copied video stream without decoding it, such as stripping Dolby Vision or HDR10+, converting Dolby Vision profile 7 to profile 8.1, or normalizing the Dolby Vision configuration record.
The decision lists every bitstream rewrite of a stream copy, and the worker applies exactly the listed rewrites to the fragmented MP4 that FFmpeg writes.
A stream copy passes the source video through unchanged apart from its listed bitstream rewrites (ADR 0036).
_Avoid_: bitstream filter (an FFmpeg component), transform, conversion (unqualified), remux

### Segment delivery

**Producer**:
The worker module that owns one job attempt's FFmpeg process, reads its standard output, and delivers that attempt's initialization segment and media segments.
_Avoid_: engine, transcoder, pipeline

**Fragment**:
One movie-fragment pair (`moof` and `mdat`) that FFmpeg emits, and the smallest unit the producer reads. A fragment starts at a keyframe or when the fragmentation target elapses. The producer never delivers a fragment as FFmpeg wrote it.
_Avoid_: chunk, part

**Rendition**:
An alternate HLS track, such as one audio language or one text subtitle track. The multivariant playlist lists each rendition in an `EXT-X-MEDIA` tag. Each rendition has its own media playlist. A viewer switches renditions without leaving the variant. A variant is the quality-ladder option that an `EXT-X-STREAM-INF` tag lists. A rendition is never a variant (ADR 0043).
_Avoid_: variant, audio variant, alternate stream, track (unqualified)

**Subtitle layer**:
A client's own view above the platform's player that draws subtitles from the original subtitle data that the server sends: text, ASS and SSA with their styling, and PGS images. A client without one gets WebVTT renditions and burned-in image subtitles (ADR 0043).
_Avoid_: overlay (unqualified), subtitle renderer, burn-in (the server's fallback)

**Audio frame grid**:
The start times of an encoded audio rendition's frames, at its encoder's frame length and sample rate, counted from the default track's first sample: the copy's own first sample when the session copies the default track, and the track's probed start when the session encodes it. Every job attempt puts each audio encode's frames on that rendition's grid, allowing for the encoder's fixed delay, so consecutive job attempts meet without a gap or an overlap. A copied track keeps its own frames, and the producer never moves it (ADR 0043).
_Avoid_: encoder frame grid, audio snap

**Lossless encode**:
An audio encode that writes every decoded sample unchanged, such as a FLAC or ALAC encode of an integer TrueHD, DTS-HD MA, PCM, FLAC or ALAC track of up to 24 bits whose layout that codec carries or pads with silent channels. A FLAC or ALAC encode of decoded lossy audio, of float or 32-bit PCM, or of a downmix is not lossless (ADR 0043).
_Avoid_: lossless for any FLAC or ALAC output

**Media segment**:
The unit an HLS playlist advertises, always fragmented MP4, delivered as one `moof` and one `mdat`. The stream session's segment list gives every segment's start, and segment N's interval runs in media time from its start up to the next segment's start. A stream session that only encodes uses the plain 3 s grid, where segment N starts at N × 3 s. A stream session with a copied variant builds its segment list from 3 s slots and merges each slot that holds no source keyframe into the segment before it. In that list, segment 0 starts at zero, every later segment starts at the exact presentation time of the source keyframe that opens it, and every encoded variant forces its keyframes there. Every variant and rendition of the stream session, and every job attempt, uses the same segment list (ADR 0037). Segment N opens at the first video keyframe at or after its start, which must lie inside its interval, even when that keyframe sits inside one of FFmpeg's fragments. Its video is every video sample from that keyframe up to the keyframe that opens a later segment. Its audio is every audio sample that starts inside its interval, whichever fragment carried it, except a sample that arrives after the segment was delivered, which the producer drops. No segment takes in an audio sample that starts after its interval, so the segment that holds the last video keyframe never takes in the audio after it. Once the server delivers audio renditions (ADR 0043), a variant's media segments carry video alone. Each audio rendition then has its own media segments on the same segment list. A subtitle rendition lists one WebVTT segment per media segment.
_Avoid_: chunk, segment file, .ts, container format

**Segment framing**:
The bytes of a delivered media segment outside its samples: the `moof` with every box inside it, and the 8-byte `mdat` header. The delivered segment layout is part of the worker contract, so the server computes a segment's framing from the segment's tracks and sample counts and never asks the worker; FFmpeg's own fragments and the fragmentation target do not change it (ADR 0037, ADR 0054).
_Avoid_: overhead, container overhead, fragment overhead

**Initialization segment**:
The `ftyp` and `moov` boxes a player needs before any media segment of a variant or an audio rendition. There is one per variant. A variant's initialization segment is identical across every attempt that the same encoder backend produces. Once the server delivers audio renditions (ADR 0043), there is also one per audio rendition. An audio rendition's initialization segment is the same whatever job attempt writes it.
_Avoid_: init file, header, init.mp4

**Media time**:
Presentation time measured from the source container's start time that the probe reports, on the zero-based timeline that every attempt of a stream session shares.
_Avoid_: PTS, source time, wall-clock time

**Start time correction**:
How far FFmpeg's command line moves a source's start time when the source's demuxer allows timestamp discontinuities, such as MPEG-TS: the earliest start time over every stream, minus the container's start time. It is zero or negative. The probe reports it, and every FFmpeg that places a job attempt's media time zero cancels it, so media time zero stays the container's start time (ADR 0037).
_Avoid_: start offset, media time zero offset, timestamp offset

**Fragmentation target**:
The configured maximum media duration of a fragment. The muxer honors it at the next packet, so it is a target, never a hard bound.
_Avoid_: fragment bound, chunk size

**Preroll**:
Preroll is media that a job attempt's seek emits before the attempt's first media segment. A stream-copy seek starts at the keyframe at or before the target. An attempt that encodes video and starts after segment 0 decodes the media before its first segment without encoding that media, so that media never reaches the producer. The producer discards preroll because this media belongs to earlier segments, except the audio lead (ADR 0055).
_Avoid_: overlap, lead-in

**Audio lead**:
The audio of the 2 s of media time before a job attempt's first media segment, which the producer delivers apart from the segments while an applied audio delay is positive. An attempt that starts at segment 0 always delivers 2 s of encoded silence as its audio lead, whatever the delay. The server shifts a segment later from it when it holds no segment before the attempt's first (ADR 0055).
_Avoid_: lead-in, preroll (which the producer discards)

**Stream copy**:
Any transcode mode that passes the source video through unchanged (REMUX and AUDIO_TRANSCODE). Its segment boundaries depend on the source's own keyframes.
_Avoid_: passthrough, remux (when only the video is copied)

**Worker-verified encoder**:
An encoder that passed a worker's test encode and that the worker advertises. It can be a hardware or a software encoder. A worker-verified encoder is not necessarily keyframe-verified.
_Avoid_: verified encoder (unqualified)

**Keyframe-verified encoder**:
An encoder is keyframe-verified when recordings under the pinned FFmpeg show that every forced keyframe is a sync sample and starts a closed GOP. The recordings must also show that the encoder restarts its GOP count at each forced keyframe. When a recording deliberately suppresses one forced keyframe, the keyframe that the encoder's GOP places instead must also start a closed GOP. Whether a particular worker can run the encoder is a separate fact, which the term worker-verified encoder describes.
_Avoid_: verified encoder (unqualified), GOP-verified encoder

**Direct play**:
The delivery in which the server sends the source file itself, and the player reads it with HTTP byte-range requests. Only Original direct-plays, and only a file whose source container, video stream, audio tracks and any selected subtitle the client capability declaration covers. A direct-play stream session starts no job attempt (ADR 0045). A player that cannot select an audio track inside the file, such as Chromium's or Firefox's, direct-plays a file only when the chosen audio track is the file's first audio track; when the viewer switches to another track, the client moves to HLS at the viewer's position (ADR 0043).
_Avoid_: direct stream, passthrough, stream copy (an HLS delivery)

**Sample entry**:
The four-character code that names a video stream's coding format in an MP4 or MOV file, such as `avc1`, `hvc1`, `hev1` or `dvh1`. Every video capability entry that lists MP4 or MOV also lists the sample entries that its player opens (ADR 0045).
_Avoid_: codec tag, FourCC (unqualified), codec string (a different value)

**Source container**:
The container of a media file as the probe found it, such as Matroska, MP4 or MPEG-TS. This source fact decides whether the file qualifies for direct play. MP4 and MOV are separate source containers, and the file's major brand tells them apart (ADR 0045). The M4V and 3GP brands, and a file without an `ftyp` box, count as MP4. MATROSKA includes WebM, and MPEG_TS includes M2TS. HLS delivery always uses fragmented MP4.
_Avoid_: container format, ContainerFormat, output container

### Jobs and library operations

**Job**:
A requested operation with an overall completion criterion, such as a library operation or a transcode job.
In library processing, a job divides into tasks, steps and attempts (ADR 0027); a transcode job has job attempts directly, with no tasks or steps.
_Avoid_: workflow, scheduled task (the db-scheduler record that runs a job)

**Library operation**:
The job that scans a library, refreshes it, or removes its items with missing files.
A library has at most one at a time, and `Library.status` shows its kind while it runs.
The mutation that requests a library operation stores the operation, and storing the operation marks the library busy.
_Avoid_: library task (a task is a portion of a job), library work (when naming one operation), claim (for storing a library operation)

**Removal**:
Removing a library's items with missing files (ADR 0038).
A removal runs as a scan's finishing step when the library's setting allows the step, or as its own library operation through `removeItemsWithMissingFiles`.
`removeLibrary` removes a whole library and is not a removal.
_Avoid_: removal (for `removeLibrary`), deletion, cleanup

**Task**:
A distinct portion of a job's work, grouped around one subject: a file in a scan, a media title in a refresh, a library item in a removal.
It is defined by its work, not by the thread that runs it, and it requires one or more steps.
_Avoid_: operation item, subtask (except the JDK's `StructuredTaskScope.Subtask` handle), work item

**Step**:
A logical operation that can be executed and retried independently, such as identify, fetch poster, fetch backdrop, probe, or fetch person or company artwork.
A step keyed by its subject, such as a file's probe, can be required by several tasks and jobs; they require it without owning it, and stopping a job never cancels it.
Streamarr aims to retry each step independently.
It already retries the probe and identify steps independently.
streamarr/streamarr-server#412 will allow Streamarr to retry artwork steps independently.
_Avoid_: phase, stage, subtask

**Attempt**:
One execution of a retryable unit of work: a step in library processing, or the transcode job itself in transcoding, where it is a job attempt.
_Avoid_: try, retry (for the first attempt), run

**Item result**:
The latest recorded outcome of a step for a subject.
An item result that counts for a library operation carries the operation's id.
It is not an attempt history: an attempt that crashes records nothing.
A soft link that a scan skips is a skipped file, not an item result.
_Avoid_: attempt history, attempt log, job result

**Skipped file**:
A file-system entry that a scan's walk skips instead of reading, stored for the library with a typed reason (ADR 0045).
The reasons are `SOFT_LINK`, `UNSUPPORTED_FILE_TYPE` (a file whose extension is not a supported video type and that the ignore rules keep) and `UNREADABLE` (an entry whose attributes the scan cannot read after it lists the entry).
A skipped file never becomes a media file, so it has no step and no item result.
A library keeps only the skipped files of its latest walk, and a ServerAdmin pages through them with `Library.administration.skippedFiles`.
_Avoid_: ignored file (a regular file whose name the ignore rules drop, which Streamarr does not store), item result (the outcome of a step), unreadable file (a file record that a scan could not read, ADR 0038), skipped item

**Take over**:
To set a library operation's id on a pending artwork request for the same item, so the operation waits on that request (ADR 0027).
The pending request may carry no operation id, as with a file-watcher request, or the id of an operation that has ended.
Taking over a request that waits to retry runs the request again at once.
The operation also sets the operation's id on the request's results of the current generation that no retry will replace.
_Avoid_: claim, adopt, reassign

**Scheduled task**:
A persisted db-scheduler task instance, as distinct from its registered task definition: the one that runs each library operation, or a step that must survive a restart, such as a probe.
Use the term only at the scheduling boundary.
_Avoid_: task (unqualified), job

**Execution**:
One run of a library operation by the operation's scheduled task.
A restart starts another execution of the same library operation, and every execution walks the library from the start (ADR 0027).
Streamarr does not record executions separately.
_Avoid_: attempt (one execution of a step or a transcode job), rerun, run (unqualified)

**Finishing step**:
An action that runs after a library operation's walk is done and every required result is recorded, such as updating file availability (ADR 0027).
The finishing steps run in a fixed order.
Each finishing step is keyed by the library operation's id and is idempotent, so a finishing step that a restart interrupted has the same effect when the finishing step runs again.
A finishing step acts on the whole library operation, while a step acts on one subject.
_Avoid_: step (unqualified), post-processing, cleanup

**Conclusion**:
How a library operation ends: finished, failed or stopped.
The conclusion decides the library's final status and which finishing steps run (ADR 0027).
_Avoid_: result (an item result belongs to a step), outcome, status (`Library.status`)

**Stop**:
A ServerAdmin's request that a running library operation admit no new tasks, let admitted tasks finish their foreground steps, and leave the library usable (ADR 0027).
`stopLibraryScan`, `stopLibraryRefresh` and `stopRemovingItemsWithMissingFiles` request a stop, and the library shows `STOPPING` until the stop completes.
A stop never cancels running work.
_Avoid_: cancel, abort, rollback

**System actor**:
The reserved identity recorded for work that no user requested, such as a scheduled library scan.
It cannot sign in, holds no Household or Profile, and a ServerAdmin who configured the work is recorded on that configuration change, not on the work.
_Avoid_: system user, anonymous, null auditor

**Batch**:
A group of items processed together.
Library operations have no batches: they admit each task on its own.
_Avoid_: page (reserved for pagination), chunk

### Attempts

**Job attempt**:
One dispatch of a variant's job to a worker, with its own FFmpeg process, producer and outcome: completed, failed or stopped.
_Avoid_: dispatch attempt, run, session

**Replacement attempt**:
A job attempt that replaces an earlier one for the same variant under ADR 0019's recovery. It keeps the variant's initialization segment, media time and encoder backend.
_Avoid_: retry, restart, replacement (unqualified)

**Relocation**:
A planned restart of the requested variant's job at the media segment that a player requested. The requested variant is the variant whose media segment the player requested. The server relocates when the requested segment lies before the running job attempt's first segment. The server also relocates when the requested segment lies more than the relocation gap ahead of the running job attempt's output. ADR 0019 defines the relocation gap. With merged segments, a segment that a held request for the same variant still waits for doesn't count toward the gap, so a prefetch never relocates (ADR 0037). Only the newest request that positioned the variant, or a newer request, may relocate the variant; an older held request whose segment needs a relocation answers 503 (ADR 0019). A relocation is not ADR 0019's recovery.
_Avoid_: seek (the player's action), restart (unqualified), recovery

**Format attempt**:
One output format that the server chooses within a playback session. Each format attempt has its own stream session. An HLS format attempt also has its own initialization segment. The initial format is the first attempt. Each Auto recovery after a format error counts as one format attempt against the playback session's format-attempt budget. A failure that the server attributes to an encoded lower variant beside a copy in Auto counts no format attempt. A new stream session that lists the copy alone then replaces the stream session, and the copy plays on while it fits the upload budget (ADR 0013). In Original, the HLS format attempt that follows a format error on a direct-played file also counts against that budget (ADR 0045). The encode that follows a format error during an HLS copy in Original or in a fixed quality also counts against that budget, and so does the copy with Dolby Vision stripped that follows a failed Dolby Vision copy. In Original, the playback choice is Auto during that encode (ADR 0052).
_Avoid_: fallback stream, replacement (unqualified)

**Encoder backend**:
An encoder that a worker runs for a codec family, together with whether it is hardware or software. A worker can run a hardware and a software backend for the same codec family, so the variants of one codec family can come from two backends at once (ADR 0044). When a connected worker has a hardware backend for the chosen codec family, the server dispatches each variant to a worker with that hardware backend first. The server dispatches the variant to a software backend when no worker whose hardware backend lists everything that the encode's output needs, such as its range, its bit depth or the reshape of Dolby Vision into PQ, can take the variant (ADR 0009, ADR 0013). A worker can take it while it has a free playback slot, and once the headroom model decides, while the model predicts that its hardware backend has headroom left (ADR 0044). Once the headroom model decides, a PQ or HLG encode on a software backend comes before the SDR encodes of hardware AV1 and HEVC backends only when the model predicts that the encode keeps the speed floor, and otherwise right after them (ADR 0013, ADR 0044). Every replacement attempt for a variant must use the backend of the job attempt that produced the variant's initialization segment (ADR 0037). When a hardware backend fails during a stream, playback continues on a new variant from the software backend of the same codec family on any worker with room, the failed worker first (ADR 0044). The new variant keeps the failed encode's range and bit depth (ADR 0013). A 10-bit SDR encode continues in 8 bits when no software backend can keep its 10 bits and a capability entry of the client accepts the 8-bit encode. When no software backend can continue a PQ or HLG encode in its range, Auto changes format within its format-attempt budget. A fixed quality then changes to an 8-bit SDR encode in a counted format attempt, when a capability entry of the client accepts that encode, and ends otherwise (ADR 0013, ADR 0052). An encoded lower variant beside a copy in Auto gets no software continuation. After the hardware backend of the lower variant that the player requests fails, a new stream session that lists the copy alone replaces the stream session (ADR 0013). A lower variant that the player has left only stops (ADR 0044). After a hardware backend fails or refuses the encode of the variant that the player requests, no later format attempt of the playback session encodes that codec family on a hardware backend (ADR 0009, ADR 0013). The worker stops offering the failed backend, and decodes in software, until the backend and the hardware decoder pass their checks again (ADR 0044).
_Avoid_: encoder capability, codec

**Completed attempt**:
A job attempt completes when FFmpeg has exited cleanly, the producer has read all output, and the server has acknowledged every delivered segment. The attempt must still be active when all three conditions hold. The server must still handle requests for any advertised segment that the attempt did not produce.
_Avoid_: finished, done, success

### Encoding load

**Pixel rate**:
Width × height × frames per second. Streamarr models, compares and reports encoding load in pixel rate.
_Avoid_: resolution or preset name as a measure of load, pixel count

**Speed floor**:
The production speed, in media seconds per second of wall-clock time, that every running variant must keep. It is the larger of 1.35× real time, below which hls.js switches down, and the neediest Apple player's floor at the stream session's segment length. At 3 s segments, the speed floor is about 2.10× real time, the floor of Safari's native player. A stream session with merged-slot segments uses its 3 s slot length. During a capacity shortfall, the requested variant keeps running even below the speed floor, and the extra variants stop (ADR 0044).
_Avoid_: hold threshold, real time (as the speed to keep)

**Encoder headroom**:
The encoding load that a worker can still take on. After the worker takes on that load, every running variant in every session that the worker serves must stay at or above the speed floor.
_Avoid_: free slots, spare capacity (unqualified)

**CPU limit**:
The share of the host's CPU that a worker and its FFmpeg processes may use: the smaller of the CPUs that the worker's CPU affinity (a container's CPU set) allows and the CPUs that its CPU quota allows. A worker with neither restriction has the whole host's CPU. The headroom model divides the CPU limit among the job attempts that encode video on that worker (ADR 0044).
_Avoid_: cores (as a setting), slots

**Calibration**:
A worker's fitted headroom constants, together with the hardware, the CPU limit and the FFmpeg build that the constants describe. The server keeps the last calibration for each combination of the three. A worker that matches a kept calibration takes jobs at once. Any other worker runs a benchmark of bounded duration first. The server refits the constants from the production speeds while sessions play, and validates each encode configuration (one encoder and preset, one source decoder, one tone-mapping chain) from its own predictions (ADR 0044).
_Avoid_: profile, benchmark (the measurement that starts a calibration)

**Production speed**:
The media seconds that a job attempt produces per second of wall-clock time. The worker measures it from each media segment's production time, which leaves out the worker's waits for earlier uploads, and reports the production time with the segment's upload (ADR 0032, ADR 0044).
_Avoid_: encode speed, transcode rate, arrival rate

**Encoder preset**:
The encoder setting that trades encoding time for compression efficiency. Streamarr names each preset on one preset scale. The preset scale maps each name to every software encoder's own preset.
The preset setting is a server setting. Every job carries its variant's preset to the worker.
A variant keeps one preset for its whole life.
_Avoid_: speed, quality setting, profile (a codec profile is a different fact)

### Worker capacity

**Capacity**:
The number of probes of one kind that a worker accepts at once: playback probes, background probes, or keyframe packet scans and bitrate scan chunks. Each worker grants its own capacities, and the server never sends a probe beyond a granted capacity (ADR 0032). A capacity of 0 accepts nothing of that kind. Job attempts have no capacity: encoder headroom decides which encodes run, and the worker's memory budget only guards against runaway output (ADR 0044, ADR 0037).
_Avoid_: limit (unqualified), quota, concurrency, playback slots, job slots

**Playback probe**:
A probe that a playback request waits for. It uses the playback probe capacity, or a free unit of the background probe capacity, and runs at normal CPU priority.
_Avoid_: urgent probe, priority probe

**Background probe**:
A probe that no playback request waits for, such as one a scan requested. It uses only the background probe capacity, which the worker adapts, and runs at the lowest CPU priority.
_Avoid_: scan probe, low-priority probe

**Keyframe packet scan**:
A probe that finds keyframe positions by scanning every packet of the chosen video stream. From probe version 9, it also reads where the chosen video stream and each audio stream ends (ADR 0032). It uses its own capacity, which is 0 while the worker runs any job attempt, and runs at the lowest CPU priority.
_Avoid_: packet scan (unqualified), keyframe probe

**Media end**:
Where the media of a stream session ends: the latest end time, as a keyframe packet scan read it, of the chosen video stream and of each audio track that the stream session delivers. An HLS stream session whose variants multiplex the audio delivers one audio track. An HLS stream session with audio renditions delivers every audio track that has a rendition (ADR 0043). A direct-play stream session delivers every audio track of the file. Until a scan has read each of those ends, the media end is the container's duration. Every variant and every audio rendition of the stream session advertises the media end, and watch progress is measured against the media end (ADR 0037). Once a timeline report of a direct-play stream session names the audio track that plays, watch progress is measured against the later end of the video and of that track (ADR 0045).
_Avoid_: real end, scanned video end, true duration

**Bitrate scan**:
A durable background step that reads every packet of a media file's chosen video stream and audio streams once, in chunks, and measures each stream's bytes in every keyframe interval. It uses the keyframe packet scan capacity, after any keyframe packet scan, and runs at the lowest CPU priority (ADR 0054).
_Avoid_: deep analysis, extensive analysis, packet scan (unqualified), bitrate probe

**Measured bitrate**:
A stream's measured bytes × 8 divided by the stream's own duration, from a measurement: an MP4 or MOV sample table, a keyframe packet scan or a bitrate scan. A measurement also holds the stream's bytes and packet count in every keyframe interval, which give the stream's peak segment bit rate. A file gets no measurement when a copy would change the size or the number of any measured stream's packets. A bitrate from Matroska statistics tags is a probed bitrate, not a measured bitrate (ADR 0054).
_Avoid_: estimated bitrate, nominal bitrate, tagged bitrate, container bitrate (the rate for the whole file)

**Brake**:
The worker's halving of its background probe capacity when a job attempt's production speed falls below the speed floor, and once when a job attempt starts (ADR 0032).
_Avoid_: throttle, backoff (unqualified)

### Catalog

**Library**:
A set of folders the server scans, together with the items built from their files.
Removing a library deletes its items but leaves their catalog entries in place.
_Avoid_: collection (a user-curated grouping), section

**Letter index**:
The number of a library's top-level items, movies or series, whose sort title starts with each letter.
The transaction that adds, removes or renames a movie or series changes the letter index (ADR 0027).
The GraphQL field `Library.alphabetIndex` returns the letter index.
_Avoid_: alphabet index (except the GraphQL field name), letter counts

**Media title**:
A movie, series, season or episode, independent of the libraries that hold it.
Use "title" as shorthand only when the context distinguishes it from the title field that holds its name.
_Avoid_: library title, item (the representation in one library), catalog entry (the durable record)

**Item**:
A media title's representation in one library, built from that library's files.
Versions of a media title inside one library share one item; libraries never share items.
_Avoid_: media title, catalog entry, metadata item

**Catalog**:
The server-wide set of catalog entries, independent of any library.
_Avoid_: library; `Catalog` as a type name (jOOQ generates one)

**Catalog entry**:
The server's durable record of a media title, person or company, whether matched to a provider or unmatched.
It holds provider ids, provider values and user-authored data, and outlives the item, person and company records that refer to it.
_Avoid_: work, title (as a type name), metadata entry, catalog item

**Credit edit**:
A metadata edit that adds or removes one person, company or genre on a movie's or series' credits.
The effective credit list is the provider's list without the removed credits, plus the added ones.
_Avoid_: cast lock, credit override

**Unmatched item**:
An item whose catalog entry has no provider id.
The `unmatched` library filter selects unmatched items.
_Avoid_: unidentified item, local item, item without an external id

**Unmatched person**:
A person with a catalog entry but no provider ids, created by a ServerAdmin with only a name while adding them to a media title's credits.
_Avoid_: local person, custom actor

**Provider id**:
One identifier that a metadata provider assigns to a media title, person or company, qualified by provider and media type, such as TMDB movie 603.
Each provider id belongs to exactly one catalog entry.
_Avoid_: GUID, external id (unqualified)

**Provider value**:
The value a metadata provider last supplied for one field of a catalog entry.
_Avoid_: original value, default value

**Metadata provider values**:
The provider ids and provider values returned together for one media title, person or company.
_Avoid_: provider result, provider entity, DTO (unqualified)

**Metadata edit**:
A value a ServerAdmin sets for one field of a catalog entry.
It applies server-wide, and refresh never discards it.
_Avoid_: override (reserved for administrative overrides in ADR 0024), lock, pin, custom value

**Effective value**:
The metadata edit for a field when one exists, otherwise the field's provider value.
It is the value shown by each attached item, person or company.
_Avoid_: shown value, display value, resolved value

**Unattached metadata edit**:
A metadata edit whose catalog entry has no current item, person or company representing it.
It is kept, and a ServerAdmin can delete it or copy it to an attached catalog entry of the same kind.
_Avoid_: orphaned edit, dangling edit

**Fix match**:
Moving an item to a different catalog entry because it matched the wrong media title.
The ServerAdmin chooses which metadata edits to copy, and watch state attributable to that item is copied automatically.
_Avoid_: rematch (unqualified), identify, unmatch

**Parsed episode numbers**:
The season number, episode number, absolute episode number or air date that Streamarr parses from a file's name or its season folder.
Sonarr calls the same parsed fields `ParsedEpisodeInfo`.
Numbering corrections attach to a file's parsed season and episode numbers, never to its file path (ADR 0047).
_Avoid_: disk coordinates, file path, TMDB numbers (the numbers that TMDB uses for the same episode), episode number (unqualified)

**Episode ordering**:
The way TMDB arranges a series' episodes into seasons.
It is TMDB's default order or one of the series' episode groups.
A ServerAdmin chooses each series' episode ordering in each library, and Streamarr never chooses it (ADR 0047).
Under an episode group, each section that holds a regular episode is the library's season item, and specials stay in Specials with their TMDB default-order season 0 numbers.
_Avoid_: display order, sort order, episode order (unqualified)

**Numbering correction**:
A ServerAdmin's rule that sends one parsed season to a TMDB season, or one parsed episode to a TMDB episode, for a series in one library.
An episode correction attaches to a parsed season and episode number, a parsed air date or a parsed absolute episode number.
An episode correction comes first, then a season correction, then the series' episode ordering (ADR 0047).
_Avoid_: metadata edit (a value on the catalog entry), override (reserved for administrative overrides in ADR 0024), mapping, remap

**File-to-episode link**:
A ServerAdmin's last-resort link from one file to one TMDB episode, for a file that its parsed episode numbers can't place.
A rename or a replacement of the file loses the link (ADR 0047).
_Avoid_: numbering correction, fix match (moving an item to another catalog entry), pin

**Numbering review**:
A library's list of the series that Streamarr marked for a ServerAdmin to review, each with its reasons, such as a better-fitting episode ordering or a broken numbering correction (ADR 0047).
The ServerAdmin triages each reason, for example by re-pointing or removing a broken numbering correction.
_Avoid_: inbox, alerts, problems (unqualified)

### Watch state

**Watch state**:
The facts that Streamarr keeps about one Profile's viewing of a catalog entry: watched status, resume position, completions, manual marks and last watched.
Watch state belongs to the Profile and the catalog entry, so every attached item shows the same watch state (ADR 0039).
The GraphQL field `selectedProfileState.watchState` returns the selected Profile's watch state of a media title (ADR 0056).
_Avoid_: watch activity, user data (unqualified), watch status (the watched status alone)

**Selected Profile state**:
What one media title holds for the selected Profile: the selected Profile itself, the Profile's watch state of the media title, and the language preferences that apply to the media title.
The GraphQL field `selectedProfileState` on a movie, series, season or episode returns the selected Profile state, and returns null when the request has no selected Profile.
Every value of a media title that depends on the selected Profile sits under `selectedProfileState` (ADR 0056).
_Avoid_: profile (unqualified), viewer state, user data (unqualified), edge (a connection edge such as `ContinueWatchingEdge`)

**Watched status**:
Whether an item is watched, in progress or unwatched for a Profile.
Only a counted resume position puts an item in progress.
A season or series derives its watched status from its episodes.
The GraphQL field `selectedProfileState.watchState.watchStatus` returns the watched status, and the `watchStatus` filter selects items by the watched status.
_Avoid_: watch status (in prose), played

**Resume position**:
Where playback of an item resumes for a Profile.
The resume position comes from the latest progress that a stream session saved for the item with a position above zero.
A resume position counts only when Streamarr saved the resume position after the item's latest completion that is not dismissed.
The GraphQL field `selectedProfileState.watchState.watchProgress` returns the resume position.
_Avoid_: watch progress (in prose), bookmark, playback position

**Start position**:
Where a playback session begins, in seconds from the start of the media. A client usually sends the resume position as the start position. Each format attempt starts at the start position until the player reports that playback moved from it. An HLS stream session starts its first job attempt at the start segment, the media segment that holds the start position, and its multivariant playlist marks the start position with `EXT-X-START` (ADR 0052).
_Avoid_: resume position (the Profile's saved position, which a client may send as the start position), seek position (a job attempt's source seek), offset

**Completion**:
A watch-history entry that says a Profile watched an item.
Playback that finishes creates a completion, and so does `markWatched`.
`markUnwatched` dismisses the item's completions.
A dismissed completion stays in watch history but no longer makes the item watched and no longer counts toward last watched.
_Avoid_: completed attempt (a job attempt that completes), play, view

**Manual mark**:
A `markWatched` or `markUnwatched` that a Profile applies to an item.
A manual mark records that item as the mark's source, and records the item's media file only when the item holds exactly one media file (ADR 0039).
A manual mark on a season or series applies to each episode.
`markUnwatched` also clears the resume positions that the mark covers.
_Avoid_: toggle, override (reserved for administrative overrides in ADR 0024)

**Last watched**:
The time a Profile last watched an item.
Last watched is the later of the time Streamarr saved the item's counted resume position and the time of the item's latest completion that is not dismissed.
A season's last watched is the latest last watched among the season's episodes, and a series' last watched is the latest last watched among the series' episodes.
Last watched orders the `LAST_WATCHED` sort and Continue Watching, and the GraphQL field `selectedProfileState.watchState.lastWatchedAt` returns the time.
_Avoid_: last played, last activity, recently watched

**Continue Watching**:
The per-Profile list of movies and episodes that have a counted resume position, ordered by last watched, newest first.
Continue Watching does not yet list a series' next episode after a Profile finishes an episode.
_Avoid_: up next (a series' next episode, which Continue Watching does not list yet), recently watched, resume list

### Notifications

**Notification**:
A stored note to one Account that something happened to a subject, such as a share offer or a library.
Each recipient Account gets its own copy when it happens, with its own read state.
A Notification is either a request, which waits for a decision and resolves for every recipient once anyone decides it or it expires or is withdrawn, or news, which needs no decision and stays unread until its recipient reads it.
A Notification never holds what waits: an offer or an invitation waits on its own, and the page that lists it counts it (ADR 0063).
A recipient has at most one unread Notification of each kind about one subject, and a later event of that kind updates it in place; news of different kinds about one subject sits side by side.
A dismissal deletes Notifications for good, such as a request that is no longer its recipient's to decide (ADR 0063); a dismissed completion, by contrast, stays in watch history.
_Avoid_: alert, inbox (neither the list of Notifications nor a list of work such as the numbering review is an inbox), message (no person writes one; the `message` field is the server's English line, as on a `MutationError`), activity feed, event (a subscription event updates an open screen and is stored nowhere), task (a portion of a job's work)

**Request**:
A Notification that waits for a decision on its subject, such as the request that tells a HouseholdAdmin that a share offer waits.
A request stores its state, which its subject's events write, and it resolves for every recipient at once when anyone decides the subject, when the subject is withdrawn, or when it expires (ADR 0063).
The word names a Notification's form only where Notifications are meant; elsewhere a request is an API request, a playback request or an artwork request.
_Avoid_: task (a portion of a job's work), action item, to-do, invitation (a ProfileManager invitation or an Account invitation, which a request can be about)

**News**:
A Notification that needs no decision, such as an offer's answer or the end of a share.
News stays unread until its recipient marks it read (ADR 0063).
_Avoid_: alert, update (what a later event of the same kind does to an unread Notification), announcement
