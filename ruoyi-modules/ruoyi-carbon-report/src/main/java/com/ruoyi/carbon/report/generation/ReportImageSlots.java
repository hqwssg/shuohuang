package com.ruoyi.carbon.report.generation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.carbon.report.domain.ReportFile;

/**
 * Cover logo, org chart and office location slots. Defaults live in the Python package assets.
 */
public final class ReportImageSlots
{
    public static final String CARBON_FILE_PREFIX = "carbon-file:";
    public static final String JOB_ASSET_PREFIX = "/job-assets/";
    public static final String DEFAULT_ASSET_PREFIX = "/assets/suning/";
    public static final Set<String> ROLES = Set.of("cover_logo", "org_chart", "office_location");

    private static final List<Slot> SLOTS = List.of(
            new Slot("cover_logo", "cover_logo.png", "image/png", "国家能源集团"),
            new Slot("org_chart", "org_chart.png", "image/png", "肃宁分公司组织机构图"),
            new Slot("office_location", "office_location.jpeg", "image/jpeg", "公司机关所在地"));

    private ReportImageSlots()
    {
    }

    public static List<Slot> slots()
    {
        return SLOTS;
    }

    public static boolean isRole(String role)
    {
        return role != null && ROLES.contains(role);
    }

    public static Slot requireSlot(String role)
    {
        for (Slot slot : SLOTS)
        {
            if (slot.role.equals(role))
            {
                return slot;
            }
        }
        throw new IllegalArgumentException("unknown image role");
    }

    public static Path defaultAssetPath(Path moduleRoot, String role)
    {
        Slot slot = requireSlot(role);
        return moduleRoot.resolve("carbon_report_agent").resolve("assets").resolve("suning").resolve(slot.filename);
    }

    public static List<Map<String, Object>> snapshotImages(List<ReportFile> uploads)
    {
        Map<String, ReportFile> byRole = new LinkedHashMap<>();
        if (uploads != null)
        {
            for (ReportFile file : uploads)
            {
                if (file != null && isRole(file.getRole()))
                {
                    byRole.put(file.getRole(), file);
                }
            }
        }
        List<Map<String, Object>> images = new ArrayList<>();
        for (Slot slot : SLOTS)
        {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("role", slot.role);
            item.put("caption", slot.caption);
            ReportFile custom = byRole.get(slot.role);
            if (custom != null && custom.getId() != null)
            {
                item.put("url", CARBON_FILE_PREFIX + custom.getId());
                item.put("mime_type", custom.getMimeType() == null || custom.getMimeType().isBlank()
                        ? slot.mimeType : custom.getMimeType());
            }
            else
            {
                item.put("url", DEFAULT_ASSET_PREFIX + slot.filename);
                item.put("mime_type", slot.mimeType);
            }
            images.add(item);
        }
        return images;
    }

    public static String materialize(String snapshotJson, Path jobAssetsDir, Function<Long, Path> resolveFile)
            throws IOException
    {
        JSONObject snapshot = snapshotJson == null || snapshotJson.isBlank()
                ? new JSONObject() : JSON.parseObject(snapshotJson);
        JSONArray images = snapshot.getJSONArray("images");
        if (images == null || images.isEmpty())
        {
            return snapshot.toJSONString();
        }
        Files.createDirectories(jobAssetsDir);
        for (int i = 0; i < images.size(); i++)
        {
            JSONObject item = images.getJSONObject(i);
            if (item == null)
            {
                continue;
            }
            String url = item.getString("url");
            if (url == null || !url.startsWith(CARBON_FILE_PREFIX))
            {
                continue;
            }
            Long fileId;
            try
            {
                fileId = Long.valueOf(url.substring(CARBON_FILE_PREFIX.length()));
            }
            catch (NumberFormatException ex)
            {
                continue;
            }
            Path source = resolveFile == null ? null : resolveFile.apply(fileId);
            if (source == null || !Files.isRegularFile(source))
            {
                continue;
            }
            String role = item.getString("role");
            String ext = extensionOf(source.getFileName().toString(), item.getString("mime_type"));
            String filename = (isRole(role) ? role : "image-" + fileId) + "." + ext;
            Path target = jobAssetsDir.resolve(filename).normalize();
            if (!target.startsWith(jobAssetsDir.toAbsolutePath().normalize()))
            {
                continue;
            }
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            item.put("url", JOB_ASSET_PREFIX + filename);
        }
        return snapshot.toJSONString();
    }

    public static String extensionOf(String originalName, String mimeType)
    {
        String name = originalName == null ? "" : originalName.toLowerCase();
        if (name.endsWith(".png"))
        {
            return "png";
        }
        if (name.endsWith(".jpg") || name.endsWith(".jpeg"))
        {
            return "jpeg";
        }
        if (mimeType != null && mimeType.toLowerCase().contains("png"))
        {
            return "png";
        }
        return "jpeg";
    }

    public static final class Slot
    {
        public final String role;
        public final String filename;
        public final String mimeType;
        public final String caption;

        private Slot(String role, String filename, String mimeType, String caption)
        {
            this.role = role;
            this.filename = filename;
            this.mimeType = mimeType;
            this.caption = caption;
        }
    }
}
