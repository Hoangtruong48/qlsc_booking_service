package com.qlsc.qlsc_booking_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class QlscBookingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(QlscBookingServiceApplication.class, args);
	}

}
