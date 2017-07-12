package cz.inqool.arclib.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;


public interface ArchivalService {
    boolean store(InputStream sip, InputStream aipXml, InputStream meta) throws IOException;
}
