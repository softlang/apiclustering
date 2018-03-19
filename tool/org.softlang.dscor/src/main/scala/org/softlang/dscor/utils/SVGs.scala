package org.softlang.dscor.utils
import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.fop.svg.PDFTranscoder;
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object SVGs {
  def convert(svg: File, pdf: File) = {
    val transcoder = new PDFTranscoder();
    val transcoderInput = new TranscoderInput(new FileInputStream(svg));
    val transcoderOutput = new TranscoderOutput(new FileOutputStream(pdf));
    transcoder.transcode(transcoderInput, transcoderOutput);
  }
}