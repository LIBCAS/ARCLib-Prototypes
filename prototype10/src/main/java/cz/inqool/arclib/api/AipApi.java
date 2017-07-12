package cz.inqool.arclib.api;

import cz.inqool.arclib.domain.AipState;
import cz.inqool.arclib.service.ArchivalService;
import cz.inqool.arclib.service.RemoteFileIdDto;
import cz.inqool.arclib.service.StorageStateDto;
import cz.inqool.uas.exception.BadArgument;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api/storage/aip")
public class AipApi {

    private ArchivalService archivalService;


    /**
     * Retrieves specified AIP
     *
     * @param uuid unique AIP identifier
     */
    @RequestMapping(value = "/{uuid}", method = RequestMethod.GET)
    public void get(@PathVariable("uuid") String uuid) {

    }

    /**
     * Stores AIP parts (SIP and ARCLib XML) into Archival Storage and verifies that data are consistent after transfer.
     * <p>
     * This endpoint also handles AIP versioning when whole AIP is versioned.
     *
     * @param sip  SIP part of AIP
     * @param xml  ARCLib XML part of AIP
     * @param meta file which contains three lines in this exact order:
     *             <ol>
     *             <li>AIP ID</li>
     *             <li>SIP MD5 hash</li>
     *             <li>ARCLib XML MD5 hash</li>
     *             </ol>
     * @return true if data are consistent, false otherwise
     */
    @RequestMapping(value = "/store", method = RequestMethod.POST)
    public boolean saveFromStream(@RequestParam("sip") MultipartFile sip, @RequestParam("xml") MultipartFile xml, @RequestParam("meta") MultipartFile meta) {
        try (InputStream sipStream = sip.getInputStream();
             InputStream xmlStream = xml.getInputStream();
             InputStream metaStream = meta.getInputStream()) {
            return archivalService.store(sipStream, xmlStream, metaStream);
        } catch (IOException e) {
            throw new BadArgument("file");
        }
    }

    /**
     * Copies ARCLib AIP XML from specified location and stores it into Archival Storage.
     *
     * @param remoteFileId contains IP of remote server and file path
     * @return
     */
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public void updateXml(@RequestBody RemoteFileIdDto remoteFileId) {
    }


    /**
     * Logically removes AIP package by setting its state to <i><removed/i>.
     *
     * @param uuid
     * @return
     */
    @RequestMapping(value = "/{uuid}", method = RequestMethod.DELETE)
    public void remove(@PathVariable("uuid") String uuid) {

    }

    /**
     * Physically removes SIP part of AIP package and setts its state to <i><deleted/i>.
     *
     * @param uuid
     * @return
     */
    @RequestMapping(value = "/{uuid}/hard", method = RequestMethod.DELETE)
    public void delete(@PathVariable("uuid") String uuid) {

    }

    /**
     * Retrieves state of AIP.
     *
     * @param uuid
     * @return
     */
    @RequestMapping(value = "/{uuid}/state", method = RequestMethod.GET)
    public AipState getAipState(@PathVariable("uuid") String uuid) {
        throw new UnsupportedOperationException();
    }


    /**
     * Retrieves state of Archival Storage: its nodes, their states etc.
     *
     * @return
     */
    @RequestMapping(value = "/state", method = RequestMethod.GET)
    public StorageStateDto getStorageState(@PathVariable("uuid") String uuid) {
        throw new UnsupportedOperationException();
    }

    @Inject
    public void setArchivalService(ArchivalService archivalService) {
        this.archivalService = archivalService;
    }
}
