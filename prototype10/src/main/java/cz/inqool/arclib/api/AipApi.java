package cz.inqool.arclib.api;

import cz.inqool.arclib.domain.AipState;
import cz.inqool.arclib.service.ArchivalService;
import cz.inqool.arclib.service.StorageStateDto;
import cz.inqool.uas.exception.BadArgument;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
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
     * @param sipId unique AIP identifier
     */
    @RequestMapping(value = "/{sipId}", method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> get(@PathVariable("sipId") String sipId) throws IOException {
        archivalService.get(sipId);
        throw new UnsupportedOperationException("not implemented yet");
    }


    /**
     * Stores AIP parts (SIP and ARCLib XML) into Archival Storage and verifies that data are consistent after transfer.
     * <p>
     * This endpoint also handles AIP versioning when whole AIP is versioned.
     * </p>
     *
     * @param sip  SIP part of AIP
     * @param xml  ARCLib XML part of AIP
     * @param meta file which contains two lines in this exact order:
     *             <ol>
     *             <li>SIP MD5 hash</li>
     *             <li>ARCLib XML MD5 hash</li>
     *             </ol>
     * @return true if data are consistent after transfer and store process, false otherwise
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
     * Stores ARCLib AIP XML into Archival Storage.
     * <p>
     * This endpoint handles AIP versioning when AIP XML is versioned.
     * </p>
     *
     * @param sipId Id of SIP to which XML belongs
     * @param xml   ARCLib XML
     * @param meta  <i>Optional</i> File containing one line with ARCLib XML MD5 hash
     * @return true if data are consistent after transfer and store process or if <i>meta</i> parameter was not set, false otherwise
     */
    @RequestMapping(value = "/{sipId}/update", method = RequestMethod.POST)
    public boolean updateXml(@PathVariable("sipId") String sipId, @RequestParam("xml") MultipartFile xml, @RequestParam(value = "meta", required = false) MultipartFile meta) {
        if (meta != null) {
            try (InputStream xmlStream = xml.getInputStream();
                 InputStream metaStream = meta.getInputStream()) {
                return archivalService.updateXml(sipId, xmlStream, metaStream);
            } catch (IOException e) {
                throw new BadArgument("file");
            }

        }
        try (InputStream xmlStream = xml.getInputStream()) {
            return archivalService.updateXml(sipId, xmlStream, null);
        } catch (IOException e) {
            throw new BadArgument("file");
        }

    }

    /**
     * Logically removes AIP package by setting its state to <i><removed/i>.
     *
     * @param sipId
     */
    @RequestMapping(value = "/{sipId}", method = RequestMethod.DELETE)
    public void remove(@PathVariable("sipId") String sipId) {
        archivalService.remove(sipId);
    }

    /**
     * Physically removes SIP part of AIP package and setts its state to <i><deleted/i>.
     *
     * @param sipId
     */
    @RequestMapping(value = "/{sipId}/hard", method = RequestMethod.DELETE)
    public void delete(@PathVariable("sipId") String sipId) throws IOException {
        archivalService.delete(sipId);
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
