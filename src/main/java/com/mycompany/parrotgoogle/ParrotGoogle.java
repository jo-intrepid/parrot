/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */

package com.mycompany.parrotgoogle;

// Imports the Google Cloud client library
import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.longrunning.OperationTimedPollAlgorithm;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.retrying.TimedRetryAlgorithm;
import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.api.gax.rpc.BidiStreamingCallable;
import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.cloud.speech.v1.LongRunningRecognizeMetadata;
import com.google.cloud.speech.v1.LongRunningRecognizeResponse;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.cloud.speech.v1.WordInfo;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import org.threeten.bp.Duration;



/**
 *
 * @author Josiah
 */
public class ParrotGoogle {

    public static void main(String[] args) {
//        System.out.println("Hello World!");
//        
//        try (SpeechClient speechClient = SpeechClient.create()) {
//
//      // The path to the audio file to transcribe
//      String gcsUri = "gs://cloud-samples-data/speech/brooklyn_bridge.raw";
//
//      // Builds the sync recognize request
//      RecognitionConfig config =
//          RecognitionConfig.newBuilder()
//              .setEncoding(AudioEncoding.LINEAR16)
//              .setSampleRateHertz(16000)
//              .setLanguageCode("en-US")
//              .build();
//      RecognitionAudio audio = RecognitionAudio.newBuilder().setUri(gcsUri).build();
//
//      // Performs speech recognition on the audio file
//      RecognizeResponse response = speechClient.recognize(config, audio);
//      List<SpeechRecognitionResult> results = response.getResultsList();
//
//      for (SpeechRecognitionResult result : results) {
//        // There can be several alternative transcripts for a given chunk of speech. Just use the
//        // first (most likely) one here.
//        SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
//        System.out.printf("Transcription: %s%n", alternative.getTranscript());
//      }
//        
//        
//    } catch (Exception e) {
//    System.out.println(e);
//        }
        while(true) {
            streamingMicRecognize(); 
        }
    }
    
      public static void syncRecognizeFile(String fileName) throws Exception {
    try (SpeechClient speech = SpeechClient.create()) {
      Path path = Paths.get(fileName);
      byte[] data = Files.readAllBytes(path);
      ByteString audioBytes = ByteString.copyFrom(data);

      // Configure request with local raw PCM audio
      RecognitionConfig config =
          RecognitionConfig.newBuilder()
              .setEncoding(AudioEncoding.LINEAR16)
              .setLanguageCode("en-US")
              .setSampleRateHertz(16000)
              .build();
      RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(audioBytes).build();

      // Use blocking call to get audio transcript
      RecognizeResponse response = speech.recognize(config, audio);
      List<SpeechRecognitionResult> results = response.getResultsList();

      for (SpeechRecognitionResult result : results) {
        // There can be several alternative transcripts for a given chunk of speech. Just use the
        // first (most likely) one here.
        SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
        System.out.printf("Transcription: %s%n", alternative.getTranscript());
      }
    }
  }
    
    public class audioDetector {
        public static Boolean isListening = true;

    }
      
    public static void streamingMicRecognize() {

    ResponseObserver<StreamingRecognizeResponse> responseObserver = null;
    try (SpeechClient client = SpeechClient.create()) {
      final String testString = "";  
      responseObserver =
          new ResponseObserver<StreamingRecognizeResponse>() {
            ArrayList<StreamingRecognizeResponse> responses = new ArrayList<>();

            public void onStart(StreamController controller) {}
            

            public void onResponse(StreamingRecognizeResponse response) {
//              response.getSpeechEventType();
//              responses.add(response);

              StreamingRecognitionResult result = response.getResultsList().get(0);
              SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
//              System.out.printf("Transcript : %s\n", alternative.getTranscript());


                audioDetector.isListening = false;
                responses.add(response);
                
               

              
            }

            public void onComplete() {
              for (StreamingRecognizeResponse response : responses) {
                StreamingRecognitionResult result = response.getResultsList().get(0);
                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                System.out.printf("Transcript : %s\n", alternative.getTranscript());
                
                playAudio(alternative.getTranscript());
                
              }
            }

            public void onError(Throwable t) {
              System.out.println(t);
            }
          };

      ClientStream<StreamingRecognizeRequest> clientStream =
          client.streamingRecognizeCallable().splitCall(responseObserver);

      RecognitionConfig recognitionConfig =
          RecognitionConfig.newBuilder()
              .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
              .setLanguageCode("en-US")
              .setSampleRateHertz(16000)
              .build();
      StreamingRecognitionConfig streamingRecognitionConfig =
//          StreamingRecognitionConfig.newBuilder().setConfig(recognitionConfig).setSingleUtterance(true).build();
            StreamingRecognitionConfig.newBuilder().setConfig(recognitionConfig).build();

      StreamingRecognizeRequest request =
          StreamingRecognizeRequest.newBuilder()
              .setStreamingConfig(streamingRecognitionConfig)
              .build(); // The first request in a streaming call has to be a config

      clientStream.send(request);
      // SampleRate:16000Hz, SampleSizeInBits: 16, Number of channels: 1, Signed: true,
      // bigEndian: false
      AudioFormat audioFormat = new AudioFormat(16000, 16, 1, true, false);
      DataLine.Info targetInfo =
          new Info(
              TargetDataLine.class,
              audioFormat); // Set the system information to read from the microphone audio stream

      if (!AudioSystem.isLineSupported(targetInfo)) {
        System.out.println("Microphone not supported");
        System.exit(0);
      }
      // Target data line captures the audio stream the microphone produces.
      TargetDataLine targetDataLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
      targetDataLine.open(audioFormat);
      targetDataLine.start();
      System.out.println("Start speaking");
      long startTime = System.currentTimeMillis();
      // Audio Input Stream
      AudioInputStream audio = new AudioInputStream(targetDataLine);
      while (audioDetector.isListening) {
        long estimatedTime = System.currentTimeMillis() - startTime;
        byte[] data = new byte[6400];
        audio.read(data);
        request =
            StreamingRecognizeRequest.newBuilder()
                .setAudioContent(ByteString.copyFrom(data))
                .build();
        clientStream.send(request);

      }
      System.out.println("Stop speaking.");
      targetDataLine.stop();
      targetDataLine.close();
      audioDetector.isListening = true;
    } catch (Exception e) {
      System.out.println(e);
    }
    responseObserver.onComplete();
  }
    
    
    public static void playAudio(String command) {
        String audioFilePath;
        
        if (command.toLowerCase().contains("god bless america")) {
            audioFilePath = "C:\\Audio\\g.wav";
            AudioPlayerExample1 player = new AudioPlayerExample1();
            player.play(audioFilePath);
        }
        else if (command.toLowerCase().contains("happy birthday")) {
            audioFilePath = "C:\\Audio\\h.wav";
            AudioPlayerExample1 player = new AudioPlayerExample1();
            player.play(audioFilePath);
        }
        else if (command.toLowerCase().contains("star spangled banner")) {
            audioFilePath = "C:\\Audio\\TheStarSpangledBanner.wav";
            AudioPlayerExample1 player = new AudioPlayerExample1();
            player.play(audioFilePath);
        }
        
        
    }
    
    public static class AudioPlayerExample1 implements LineListener {
     
    /**
     * this flag indicates whether the playback completes or not.
     */
    boolean playCompleted;
     
    /**
     * Play a given audio file.
     * @param audioFilePath Path of the audio file.
     */
    void play(String audioFilePath) {
        File audioFile = new File(audioFilePath);
 
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
 
            AudioFormat format = audioStream.getFormat();
 
            DataLine.Info info = new DataLine.Info(Clip.class, format);
 
            Clip audioClip = (Clip) AudioSystem.getLine(info);
 
            audioClip.addLineListener(this);
 
            audioClip.open(audioStream);
             
            audioClip.start();
             
            while (!playCompleted) {
                // wait for the playback completes
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
             
            audioClip.close();
             
        } catch (UnsupportedAudioFileException ex) {
            System.out.println("The specified audio file is not supported.");
            ex.printStackTrace();
        } catch (LineUnavailableException ex) {
            System.out.println("Audio line for playing back is unavailable.");
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("Error playing the audio file.");
            ex.printStackTrace();
        }
         
    }
     
    /**
     * Listens to the START and STOP events of the audio line.
     */
    @Override
    public void update(LineEvent event) {
        LineEvent.Type type = event.getType();
         
        if (type == LineEvent.Type.START) {
            System.out.println("Playback started.");
             
        } else if (type == LineEvent.Type.STOP) {
            playCompleted = true;
            System.out.println("Playback completed.");
        }
 
    }
}
}

