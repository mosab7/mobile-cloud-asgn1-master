package org.magnum.dataup;


import net.bytebuddy.implementation.bytecode.Throw;
import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Controller
public class VideoController {
    public static final String VIDEO_SVC_PATH = "/video";
    public static final String VIDEO_DATA_PATH = VIDEO_SVC_PATH + "/{id}/data";
    private static final AtomicLong currentId = new AtomicLong(0L);

    private Map<Long, Video> videos = new HashMap<Long, Video>();

    @RequestMapping(value = VIDEO_SVC_PATH, method = RequestMethod.GET)
    public @ResponseBody
    Collection<Video> getVideoList() {
        return videos.values();
    }

    @RequestMapping(value = VIDEO_DATA_PATH, method = RequestMethod.GET)
    public void getData(@PathVariable("id") long id, HttpServletResponse response)
            throws IOException {
        VideoFileManager videoData = VideoFileManager.get();

        try {
            videoData.copyVideoData(videos.get(id), response.getOutputStream());
        } catch (Exception e) {
            // Using NoSuchElementException//IllegalArgumentException will return 500 instead of 404.
            throw new ResourceNotFoundException();
        }
    }

    @RequestMapping(value = VIDEO_SVC_PATH, method = RequestMethod.POST)
    public @ResponseBody Video addVideoMetadata(@RequestBody Video v, HttpServletRequest request)
            throws IOException {
        v.setId(currentId.incrementAndGet());
        v.setDataUrl(getUrlBaseForLocalServer(request) + "/" + VIDEO_SVC_PATH + v.getId() + "/data");
        videos.put(v.getId(), v);
        return v;
    }

    @RequestMapping(value = VIDEO_DATA_PATH, method = RequestMethod.POST)
    public @ResponseBody VideoStatus addVideoData(@PathVariable("id") long id,
                                                  @RequestParam MultipartFile data) throws IOException {
        VideoFileManager videoData = VideoFileManager.get();
        try {
            videoData.saveVideoData(videos.get(id), data.getInputStream());
        } catch (Exception e) {
            throw new ResourceNotFoundException();
        }
        return new VideoStatus(VideoStatus.VideoState.READY);
    }

    private String getUrlBaseForLocalServer(HttpServletRequest request) {
        String baseURL = "http://" + request.getServerName()
                + ((request.getServerPort() != 80) ? ":" + request.getServerPort() : "");
        return baseURL;
    }
}
