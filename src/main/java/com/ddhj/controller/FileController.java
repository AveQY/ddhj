package com.ddhj.controller;

import com.ddhj.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Tag(name = "文件上传")
@RestController
@RequestMapping("/upload")
public class FileController {

    private static final String UPLOAD_DIR = "image/";

    @Operation(summary = "上传图片")
    @PostMapping
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        try {
            // 获取项目根目录的绝对路径
            String projectPath = System.getProperty("user.dir");
            File uploadDir = new File(projectPath, UPLOAD_DIR);

            // 创建上传目录
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // 保存并压缩文件
            File dest = new File(uploadDir, filename);

            // 使用 Thumbnailator 进行压缩
            // scale(1f) 表示保持原图尺寸，outputQuality(0.8f) 表示设置图片质量为 80%
            net.coobird.thumbnailator.Thumbnails.of(file.getInputStream())
                    .scale(1f)
                    .outputQuality(0.8f)
                    .toFile(dest);

            // 返回访问路径
            String filePath = "/api/image/" + filename;
            return Result.success(filePath);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
}
