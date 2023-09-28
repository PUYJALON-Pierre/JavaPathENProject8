package tourGuide;

import java.util.Locale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main class of TourGuide Application
 * 
 */
@SpringBootApplication
public class Application {

	public static void main(String[] args) {

		Locale.setDefault(Locale.ENGLISH);
		SpringApplication.run(Application.class, args);
	}

}
