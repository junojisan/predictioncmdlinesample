package com.gmail.jimaoka.main;

import java.io.IOException;

import com.gmail.jimaoka.google.api.service.prediction.PredictionService;

/**
 * Prediction API コマンドラインアプリ
 * @author junji.imaoka
 */
public class PredictionCmdLineSampleApp {	
	public static void main(String[] args) {
		try{
			new PredictionService().run();
		}catch(IOException e){
			System.err.println(e.getMessage());
		}catch(Throwable t){
			t.printStackTrace();
		}
	}
}
