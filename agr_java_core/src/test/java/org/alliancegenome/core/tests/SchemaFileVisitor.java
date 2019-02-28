package org.alliancegenome.core.tests;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class SchemaFileVisitor implements FileVisitor<Path> {

    List<Path> fileList = new ArrayList<Path>();
    
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        //System.out.println("preVisitDirectory: " + dir);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        //System.out.println("visitFile: " + file);
        if(file.getFileName().toString().endsWith(".json")) {
            fileList.add(file);
            //System.out.println(fileList);
        } else {
            //System.out.println("File does not end with json");
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        //System.out.println("visitFileFailed: " + file);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        //System.out.println("postVisitDirectory: " + dir);
        return FileVisitResult.CONTINUE;
    }

    public List<Path> getFileList() {
        return fileList;
    }

}
