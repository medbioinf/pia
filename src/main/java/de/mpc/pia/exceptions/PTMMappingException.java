package de.mpc.pia.exceptions;

/**
 * This code is licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * This error througth an exception for PTMs that can't be handle by the PIA.
 * <p>
 * This class
 * <p>
 * Created by ypriverol (ypriverol@gmail.com) on 17/10/2017.
 */
public class PTMMappingException extends Exception {

    public PTMMappingException(String oldAccession){
        super("An error has been produced mapping the following PTM: " + oldAccession);
    }
}
