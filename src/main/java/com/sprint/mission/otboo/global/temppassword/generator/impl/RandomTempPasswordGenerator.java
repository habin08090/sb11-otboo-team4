package com.sprint.mission.otboo.global.temppassword.generator.impl;

import com.sprint.mission.otboo.global.temppassword.generator.TempPasswordGenerator;
import java.security.SecureRandom;

public class RandomTempPasswordGenerator implements TempPasswordGenerator {

  private static final int PASSWORD_LENGTH = 12;
  // 혼동되기 쉬운 문자(I, O, l, 0, 1)를 제외해 가독성/오타 방지
  private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
  private static final String DIGITS = "23456789";
  private static final String SPECIALS = "!@$";
  private static final String CHARS = LETTERS + DIGITS + SPECIALS;
  private final SecureRandom random = new SecureRandom();

  @Override
  public String generate() {
    char[] password = new char[PASSWORD_LENGTH];
    password[0] = LETTERS.charAt(random.nextInt(LETTERS.length()));
    password[1] = DIGITS.charAt(random.nextInt(DIGITS.length()));
    for (int i = 2; i < PASSWORD_LENGTH; i++) {
      password[i] = CHARS.charAt(random.nextInt(CHARS.length()));
    }
    for (int i = password.length - 1; i > 0; i--) {
      int j = random.nextInt(i + 1);
      char temp = password[i];
      password[i] = password[j];
      password[j] = temp;
    }
    return new String(password);
  }
}
