package com.hipad.controller;

import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
//@RequestMapping(value = "/file")
public class FileUploadController {
    //Save the uploaded file to this folder
    private static String UPLOADED_FOLDER = "E://temp//";

    @GetMapping("/")
    public String index() {
        return "upload";
    }

    @GetMapping("/from")
    public String from() {
        return "from_file";
    }

    @PostMapping("/upload") // //new annotation since 4.3
    public String singleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please select a file to upload");
            return "redirect:uploadStatus";
        }

        try {
            // Get the file and save it somewhere
            byte[] bytes = file.getBytes();
            Path path = Paths.get(UPLOADED_FOLDER + file.getOriginalFilename());
            Files.write(path, bytes);

            redirectAttributes.addFlashAttribute("message",
                    "You successfully uploaded '" + file.getOriginalFilename() + "'");

        } catch (IOException e) {
            e.printStackTrace();
        }

        return "redirect:/uploadStatus";
    }

    @GetMapping("/uploadStatus")
    public String uploadStatus() {
        return "uploadStatus";
    }

//    @RequestMapping(value = "/upload")
//    @ResponseBody
//    public String handleFileUpload(@RequestParam(value = "file", required = true) MultipartFile file) throws IOException {
//        byte[] bytes = file.getBytes();
//        File fileToSave = new File(file.getOriginalFilename());
//        FileCopyUtils.copy(bytes, fileToSave);
//        return fileToSave.getAbsolutePath();
//    }


}
