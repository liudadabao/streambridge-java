package io.github.streambridge.engine.zlm;

import com.sun.jna.Library;
import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

interface ZlmNativeApi extends Library {
    interface MediaSourceCallback extends Callback { void invoke(Pointer userData, Pointer source); }
    interface ProxyCallback extends Callback { void invoke(Pointer userData, int error, String message, int systemError); }
    interface PushCallback extends Callback { void invoke(Pointer userData, int error, String message); }
    interface RtpDetachCallback extends Callback { void invoke(Pointer userData); }
    interface WebRtcCallback extends Callback { void invoke(Pointer userData, String answer, String error); }
    interface MediaCloseCallback extends Callback { void invoke(Pointer userData); }
    interface PlayerCallback extends Callback { void invoke(Pointer userData, int error, String message, Pointer tracks, int trackCount); }
    interface TrackFrameCallback extends Callback { void invoke(Pointer userData, Pointer frame); }
    interface MediaChangedCallback extends Callback { void invoke(int registered, Pointer source); }

    final class Events extends Structure {
        public MediaChangedCallback on_mk_media_changed;
        public Pointer on_mk_media_publish;
        public Pointer on_mk_media_play;
        public Pointer on_mk_media_not_found;
        public Pointer on_mk_media_no_reader;
        public Pointer on_mk_http_request;
        public Pointer on_mk_http_access;
        public Pointer on_mk_http_before_access;
        public Pointer on_mk_rtsp_get_realm;
        public Pointer on_mk_rtsp_auth;
        public Pointer on_mk_record_mp4;
        public Pointer on_mk_record_ts;
        public Pointer on_mk_shell_login;
        public Pointer on_mk_flow_report;
        public Pointer on_mk_log;
        public Pointer on_mk_media_send_rtp_stop;
        public Pointer on_mk_rtc_sctp_connecting;
        public Pointer on_mk_rtc_sctp_connected;
        public Pointer on_mk_rtc_sctp_failed;
        public Pointer on_mk_rtc_sctp_closed;
        public Pointer on_mk_rtc_sctp_send;
        public Pointer on_mk_rtc_sctp_received;

        @Override protected List<String> getFieldOrder() {
            return Arrays.asList("on_mk_media_changed", "on_mk_media_publish", "on_mk_media_play",
                "on_mk_media_not_found", "on_mk_media_no_reader", "on_mk_http_request", "on_mk_http_access",
                "on_mk_http_before_access", "on_mk_rtsp_get_realm", "on_mk_rtsp_auth", "on_mk_record_mp4",
                "on_mk_record_ts", "on_mk_shell_login", "on_mk_flow_report", "on_mk_log",
                "on_mk_media_send_rtp_stop", "on_mk_rtc_sctp_connecting", "on_mk_rtc_sctp_connected",
                "on_mk_rtc_sctp_failed", "on_mk_rtc_sctp_closed", "on_mk_rtc_sctp_send", "on_mk_rtc_sctp_received");
        }
    }
    void mk_env_init2(
        int threadNum,
        int logLevel,
        int logMask,
        String logFilePath,
        int logFileDays,
        int iniIsPath,
        String ini,
        int sslIsPath,
        String ssl,
        String sslPassword
    );

    void mk_stop_all_server();

    short mk_http_server_start(short port, int ssl);

    short mk_rtsp_server_start(short port, int ssl);

    short mk_rtmp_server_start(short port, int ssl);

    void mk_media_source_for_each(Pointer userData, MediaSourceCallback callback, String schema, String vhost, String app, String stream);
    String mk_media_source_get_schema(Pointer source);
    String mk_media_source_get_vhost(Pointer source);
    String mk_media_source_get_app(Pointer source);
    String mk_media_source_get_stream(Pointer source);
    String mk_media_source_get_origin_url(Pointer source);
    int mk_media_source_get_reader_count(Pointer source);
    int mk_media_source_get_total_reader_count(Pointer source);
    int mk_media_source_get_bytes_speed(Pointer source);
    long mk_media_source_get_alive_second(Pointer source);
    int mk_media_source_close(Pointer source, int force);

    Pointer mk_proxy_player_create3(String vhost, String app, String stream, int hlsEnabled, int mp4Enabled, int retryCount);
    void mk_proxy_player_set_option(Pointer player, String key, String value);
    void mk_proxy_player_set_on_close(Pointer player, ProxyCallback callback, Pointer userData);
    void mk_proxy_player_set_on_play_result(Pointer player, ProxyCallback callback, Pointer userData, Pointer userDataFree);
    void mk_proxy_player_play(Pointer player, String url);
    void mk_proxy_player_release(Pointer player);

    Pointer mk_pusher_create(String schema, String vhost, String app, String stream);
    void mk_pusher_set_option(Pointer pusher, String key, String value);
    void mk_pusher_set_on_result(Pointer pusher, PushCallback callback, Pointer userData);
    void mk_pusher_set_on_shutdown(Pointer pusher, PushCallback callback, Pointer userData);
    void mk_pusher_publish(Pointer pusher, String url);
    void mk_pusher_release(Pointer pusher);

    int mk_recorder_is_recording(int type, String vhost, String app, String stream);
    int mk_recorder_start(int type, String vhost, String app, String stream, String path, long maxSeconds);
    int mk_recorder_stop(int type, String vhost, String app, String stream);

    Pointer mk_rtp_server_create3(short port, int tcpMode, String vhost, String app, String stream, int multiplex);
    void mk_rtp_server_release(Pointer server);
    short mk_rtp_server_port(Pointer server);
    void mk_rtp_server_set_on_detach(Pointer server, RtpDetachCallback callback, Pointer userData);
    void mk_rtp_server_update_ssrc(Pointer server, int ssrc);

    void mk_webrtc_get_answer_sdp(Pointer userData, WebRtcCallback callback, String type, String offer, String url);

    Pointer mk_media_create(String vhost, String app, String stream, float duration, int hlsEnabled, int mp4Enabled);
    void mk_media_release(Pointer media);
    int mk_media_init_video(Pointer media, int codec, int width, int height, float fps, int bitRate);
    int mk_media_init_audio(Pointer media, int codec, int sampleRate, int channels, int sampleBits);
    void mk_media_init_complete(Pointer media);
    void mk_media_set_on_close(Pointer media, MediaCloseCallback callback, Pointer userData);
    int mk_media_input_frame(Pointer media, Pointer frame);
    int mk_media_total_reader_count(Pointer media);
    Pointer mk_frame_create(int codec, long dts, long pts, byte[] data, long size, Pointer releaseCallback, Pointer userData);
    void mk_frame_unref(Pointer frame);
    void mk_set_option(String key, String value);
    String mk_get_option(String key);
    void mk_events_listen(Events events);

    Pointer mk_player_create();
    void mk_player_release(Pointer player);
    void mk_player_set_option(Pointer player, String key, String value);
    void mk_player_play(Pointer player, String url);
    void mk_player_pause(Pointer player, int pause);
    void mk_player_speed(Pointer player, float speed);
    void mk_player_seekto(Pointer player, float progress);
    void mk_player_seekto_pos(Pointer player, int seconds);
    void mk_player_set_on_result(Pointer player, PlayerCallback callback, Pointer userData);
    void mk_player_set_on_shutdown(Pointer player, PlayerCallback callback, Pointer userData);
    float mk_player_duration(Pointer player);
    float mk_player_progress(Pointer player);
    int mk_player_progress_pos(Pointer player);
    float mk_player_loss_rate(Pointer player, int trackType);
    Pointer mk_track_add_delegate(Pointer track, TrackFrameCallback callback, Pointer userData);
    void mk_track_del_delegate(Pointer track, Pointer tag);
    int mk_frame_codec_id(Pointer frame);
    Pointer mk_frame_get_data(Pointer frame);
    long mk_frame_get_data_size(Pointer frame);
    long mk_frame_get_dts(Pointer frame);
    long mk_frame_get_pts(Pointer frame);
}
