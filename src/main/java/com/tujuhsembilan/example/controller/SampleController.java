package com.tujuhsembilan.example.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sample")
public class SampleController {

  @GetMapping("/data-a")
  public ResponseEntity<?> getDataA() {
    return ResponseEntity.ok().build();
  }

  @GetMapping("/data-b")
  public ResponseEntity<?> getDataB() {
    return ResponseEntity.ok().build();
  }

  @GetMapping("/data-c")
  public ResponseEntity<?> getDataC() {
    return ResponseEntity.ok().build();
  }

}
