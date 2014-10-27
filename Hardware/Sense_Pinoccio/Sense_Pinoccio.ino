/**************************************************************************\
* Pinoccio Library                                                         *
* https://github.com/Pinoccio/library-pinoccio                             *
* Copyright (c) 2014, Pinoccio Inc. All rights reserved.                   *
* ------------------------------------------------------------------------ *
*  This program is free software; you can redistribute it and/or modify it *
*  under the terms of the MIT License as described in license.txt.         *
\**************************************************************************/
#include <SPI.h>
#include <Wire.h>
#include <Scout.h>
#include <GS.h>
#include <bitlash.h>
#include <lwm.h>
#include <js0n.h>

#include "version.h"

int sensorPin = 2;
int value = 0;

void setup() {
  Scout.setup(SKETCH_NAME, SKETCH_REVISION, SKETCH_BUILD);
  // Add custom setup code here
  Serial.begin (9600);

  Scout.setMode(sensorPin, Scout.PINMODE_INPUT);
}

void loop() {
  Scout.loop();
  // Add custom loop code here
//  value = analogRead (sensorPin);
  value = Scout.pinRead(sensorPin);
  Serial.println (value, DEC);
  delay (250);

}
