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

### Probe

**Probe properties**:
The technical facts about a media file's container and streams that a probe reports. Stream properties are the facts about one stream.
_Avoid_: metadata, technical metadata

**Metadata**:
Descriptive data about a media title, person or company, such as names, artwork and credits.
_Avoid_: metadata for technical facts, probe metadata

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
An encode's codec string follows from the encode's codec family and from the codec profile, level, tier and bit depth that the encode forces.
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

**RPU placement**:
Where the samples of an H.264 or HEVC Dolby Vision video stream put each picture's RPU, the unit that holds the picture's Dolby Vision reshaping instructions.
The probe reports it from the first packets: each RPU follows its own picture, every RPU starts its sample, or unspecified when the probe cannot tell.
Every RPU starts its sample in two layouts: the shifted layout, where each picture's RPU starts the next picture's sample, and the own-first layout, where it starts its own picture's sample.
One value covers both, because the first packets of a shifted file whose start was cut away look like an own-first file's, and both take the same repair.
The placement rules out a copy when every RPU starts its sample, and never when it is unspecified.
The worker reads the placement before an encode that must reshape the pictures, to move each RPU behind the picture that it belongs to, under the conditions that ADR 0013 sets.
_Avoid_: RPU order, RPU position, RPU offset, RPU layout for the reported value (a layout is the arrangement in a file that the placement reports)

**Output frame rate**:
The frame rate at which every encode of a chosen video stream runs.
The probe outcome, the transcode job and the worker's FFmpeg argument carry the output frame rate as an exact fraction, never as a decimal.
The probe job chooses the output frame rate once per file.
Every job attempt therefore encodes on one frame grid, which is the same sequence of output frame times.
_Avoid_: target frame rate, frame rate (unqualified) for the encode's rate

### Playback records

**Playback session**:
The logical span of one viewing of a media title, which survives replacement stream sessions and holds the format-failure history and the format-attempt budget.
_Avoid_: playback (ADR 0018's token and request surface), app session, stream session (one server-side run inside it)

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
_Avoid_: codec profile (a profile of the coding standard), device profile, condition

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
One movie-fragment pair (`moof` and `mdat`) that FFmpeg emits, and the smallest unit the producer reads. A fragment starts at a keyframe or when the fragmentation target elapses. The producer never delivers a fragment as FFmpeg wrote it.
_Avoid_: chunk, part

**Rendition**:
An alternate HLS track, such as one audio language or one text subtitle track. The multivariant playlist lists each rendition in an `EXT-X-MEDIA` tag. Each rendition has its own media playlist. A viewer switches renditions without leaving the variant. A variant is the quality-ladder option that an `EXT-X-STREAM-INF` tag lists. A rendition is never a variant (ADR 0043).
_Avoid_: variant, audio variant, alternate stream, track (unqualified)

**Media segment**:
The unit an HLS playlist advertises, always fragmented MP4, delivered as one `moof` and one `mdat`. The stream session's segment list gives every segment's start, and segment N's interval runs in media time from its start up to the next segment's start. A stream session that only encodes uses the plain 3 s grid, where segment N starts at N × 3 s. A stream session with a copied variant builds its segment list from 3 s slots and merges each slot that holds no source keyframe into the segment before it. Until merged segment lists ship ([streamarr/streamarr-server#512](https://github.com/streamarr/streamarr-server/issues/512)), ADR 0037's interim rule applies instead. Under the interim rule, a stream session whose every variant is a stream copy keeps 6 s segments on the plain 6 s grid. Every other stream session uses the plain 3 s grid. In a stream session with a copied variant, a final grid interval that holds no source keyframe joins the segment before it, so the last segment can last up to twice the period. Segment N opens at the first video keyframe at or after its start, which must lie inside its interval, even when that keyframe sits inside one of FFmpeg's fragments. Its video is every video sample from that keyframe up to the keyframe that opens a later segment. Its audio is every audio sample that starts inside its interval, whichever fragment carried it, except a sample that arrives after the segment was delivered, which the producer drops. Once the server delivers audio renditions (ADR 0043), a variant's media segments carry video alone. Each audio rendition then has its own media segments on the same segment list. A subtitle rendition lists one WebVTT segment per media segment.
_Avoid_: chunk, segment file, .ts, container format

**Initialization segment**:
The `ftyp` and `moov` boxes a player needs before any media segment of a variant or an audio rendition. There is one per variant. A variant's initialization segment is identical across every attempt that the same encoder backend produces. Once the server delivers audio renditions (ADR 0043), there is also one per audio rendition. An audio rendition's initialization segment is the same whatever job attempt writes it.
_Avoid_: init file, header, init.mp4

**Media time**:
Presentation time measured from the source container's start time that the probe reports, on the zero-based timeline that every attempt of a stream session shares.
_Avoid_: PTS, source time, wall-clock time

**Start time correction**:
How far FFmpeg's command line moves a source's start time when the source's demuxer allows timestamp discontinuities, such as MPEG-TS: the earliest start time over every stream, minus the container's start time. It is zero or negative. The probe reports it, and every FFmpeg that reads the source for a job attempt cancels it, so media time zero stays the container's start time (ADR 0037).
_Avoid_: start offset, media time zero offset, timestamp offset

**Fragmentation target**:
The configured maximum media duration of a fragment. The muxer honors it at the next packet, so it is a target, never a hard bound.
_Avoid_: fragment bound, chunk size

**Preroll**:
Preroll is media that a job attempt's seek emits before the attempt's first media segment. A stream-copy seek starts at the keyframe at or before the target. An attempt that encodes video and starts after segment 0 decodes the media before its first segment without encoding that media, so that media never reaches the producer. The producer discards preroll because this media belongs to earlier segments.
_Avoid_: overlap, lead-in

**Stream copy**:
Any transcode mode that passes the source video through unchanged (REMUX and AUDIO_TRANSCODE). Its segment boundaries depend on the source's own keyframes.
_Avoid_: passthrough, remux (when only the video is copied)

**Worker-verified encoder**:
An encoder that passed a worker's test encode and that the worker advertises. It can be a hardware or a software encoder. A worker-verified encoder is not necessarily keyframe-verified.
_Avoid_: verified encoder (unqualified)

**Keyframe-verified encoder**:
An encoder is keyframe-verified when recordings under the pinned FFmpeg show that every forced keyframe is a sync sample and starts a closed GOP. The recordings must also show that the encoder restarts its GOP count at each forced keyframe. When a recording deliberately suppresses one forced keyframe, the keyframe that the encoder's GOP places instead must also start a closed GOP. Whether a particular worker can run the encoder is a separate fact, which the term worker-verified encoder describes.
_Avoid_: verified encoder (unqualified), GOP-verified encoder

**Source container**:
The container of a media file as the probe found it, such as Matroska, MP4 or MPEG-TS. This source fact decides whether the file qualifies for direct play. HLS delivery always uses fragmented MP4.
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
_Avoid_: attempt history, attempt log, job result

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
A planned restart of the requested variant's job at the media segment that a player requested. The requested variant is the variant whose media segment the player requested. The server relocates when the requested segment lies before the running job attempt's first segment. The server also relocates when the requested segment lies more than the relocation gap ahead of the running job attempt's output. ADR 0019 defines the relocation gap. Only the newest request that positioned the variant, or a newer request, may relocate the variant; an older held request whose segment needs a relocation answers 503 (ADR 0019). A relocation is not ADR 0019's recovery.
_Avoid_: seek (the player's action), restart (unqualified), recovery

**Format attempt**:
One output format that the server chooses within a playback session. Each format attempt has its own stream session. An HLS format attempt also has its own initialization segment. The initial format is the first attempt. Each Auto recovery after a format error counts as one format attempt against the playback session's format-attempt budget. In Original, the HLS format attempt that follows a format error on a direct-played file also counts against that budget (ADR 0045).
_Avoid_: fallback stream, replacement (unqualified)

**Encoder backend**:
An encoder that a worker runs for a codec family, together with whether it is hardware or software. A worker can run a hardware and a software backend for the same codec family, so the variants of one codec family can come from two backends at once (ADR 0044). Every replacement attempt for a variant must use the backend of the job attempt that produced the variant's initialization segment (ADR 0037).
_Avoid_: encoder capability, codec

**Completed attempt**:
A job attempt completes when FFmpeg has exited cleanly, the producer has read all output, and the server has acknowledged every delivered segment. The attempt must still be active when all three conditions hold. The server must still handle requests for any advertised segment that the attempt did not produce.
_Avoid_: finished, done, success

### Encoding load

**Pixel rate**:
Width × height × frames per second. Streamarr models, compares and reports encoding load in pixel rate.
_Avoid_: resolution or preset name as a measure of load, pixel count

**Speed floor**:
The production speed, in media seconds per second of wall-clock time, that every running variant must keep. It is the larger of 1.35× real time, below which hls.js switches down, and the neediest Apple player's floor at the stream session's segment length. At 3 s segments, the speed floor is about 2.10× real time, the floor of Safari's native player. A stream session with merged-slot segments uses its 3 s slot length.
_Avoid_: hold threshold, real time (as the speed to keep)

**Encoder headroom**:
The encoding load that a worker can still take on. After the worker takes on that load, every running variant in every session that the worker serves must stay at or above the speed floor.
_Avoid_: free slots, spare capacity (unqualified)

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
The number of items of one kind that a worker accepts at once. Each worker grants its own capacities, and the server never sends an item beyond a granted capacity (ADR 0032). A capacity of 0 accepts nothing of that kind.
_Avoid_: limit (unqualified), quota, concurrency

**Playback slots**:
The capacity for job attempts. A worker setting fixes it until encoder headroom replaces it. Probes never use a playback slot.
_Avoid_: slots for probes, shared slots, free slots (as encoder headroom)

**Playback probe**:
A probe that a playback request waits for. It uses the playback probe capacity, or a free unit of the background probe capacity, and runs at normal CPU priority.
_Avoid_: urgent probe, priority probe

**Background probe**:
A probe that no playback request waits for, such as one a scan requested. It uses only the background probe capacity, which the worker adapts, and runs at the lowest CPU priority.
_Avoid_: scan probe, low-priority probe

**Keyframe packet scan**:
A probe that finds keyframe positions by scanning every packet of the chosen video stream. It uses its own capacity, which is 0 while the worker runs any job attempt, and runs at the lowest CPU priority.
_Avoid_: packet scan (unqualified), keyframe probe

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

### Watch state

**Watch state**:
The facts that Streamarr keeps about one Profile's viewing of a catalog entry: watched status, resume position, completions, manual marks and last watched.
Watch state belongs to the Profile and the catalog entry, so every attached item shows the same watch state (ADR 0039).
_Avoid_: watch activity, user data (unqualified), watch status (the watched status alone)

**Watched status**:
Whether an item is watched, in progress or unwatched for a Profile.
Only a counted resume position puts an item in progress.
A season or series derives its watched status from its episodes.
The GraphQL field `watchStatus` returns the watched status, and the `watchStatus` filter selects items by the watched status.
_Avoid_: watch status (in prose), played

**Resume position**:
Where playback of an item resumes for a Profile.
The resume position comes from the latest progress that a stream session saved for the item with a position above zero.
A resume position counts only when Streamarr saved the resume position after the item's latest completion that is not dismissed.
The GraphQL field `watchProgress` returns the resume position.
_Avoid_: watch progress (in prose), bookmark, playback position

**Completion**:
A watch-history entry that says a Profile watched an item.
Playback that finishes creates a completion, and so does `markWatched`.
`markUnwatched` dismisses the item's completions.
A dismissed completion stays in watch history but no longer makes the item watched and no longer counts toward last watched.
_Avoid_: completed attempt (a job attempt that completes), play, view

**Manual mark**:
A `markWatched` or `markUnwatched` that a Profile applies to an item.
A manual mark records that item as the mark's source (ADR 0039).
A manual mark on a season or series applies to each episode.
`markUnwatched` also clears the resume positions that the mark covers.
_Avoid_: toggle, override (reserved for administrative overrides in ADR 0024)

**Last watched**:
The time a Profile last watched an item.
Last watched is the later of the time Streamarr saved the item's counted resume position and the time of the item's latest completion that is not dismissed.
A series' last watched is the latest last watched among the series' episodes.
Last watched orders the `LAST_WATCHED` sort and Continue Watching, and the GraphQL field `lastWatchedAt` returns the time.
_Avoid_: last played, last activity, recently watched

**Continue Watching**:
The per-Profile list of movies and episodes that have a counted resume position, ordered by last watched, newest first.
Continue Watching does not yet list a series' next episode after a Profile finishes an episode.
_Avoid_: up next (a series' next episode, which Continue Watching does not list yet), recently watched, resume list
