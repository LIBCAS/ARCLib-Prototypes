package cz.inqool.arclib.service;

import cz.inqool.arclib.fixity.FixityCounter;
import cz.inqool.uas.file.FileRepository;
import cz.inqool.uas.store.Transactional;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Service
@Transactional
public class FileSystemArchivalService implements ArchivalService {

    private FileRepository repository;
    private FixityCounter fixityCounter;

    public boolean store(InputStream sip, InputStream aipXml, InputStream meta) throws IOException {
        String id;
        String sipHash;
        String xmlHash;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(meta))) {
            id = br.readLine();
            sipHash = br.readLine();
            xmlHash = br.readLine();
        }
        String sipId = "sip_" + id;
        String xmlId = "xml_1_" + id;
        repository.create(sip, sipId, "application/x-tar", false);
        repository.create(aipXml, xmlId, "application/xml", true);
        return fixityCounter.verifyFixity(repository.get(sipId).getStream(), sipHash) && fixityCounter.verifyFixity(repository.get(xmlId).getStream(), xmlHash);
    }

    @Inject
    public void setRepository(FileRepository repository) {
        this.repository = repository;
    }

    @Inject
    public void setFixityCounter(FixityCounter fixityCounter) {
        this.fixityCounter = fixityCounter;
    }
}
