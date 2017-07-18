package cz.inqool.arclib.api;

import cz.inqool.arclib.domain.AipState;
import cz.inqool.arclib.dto.StorageStateDto;
import cz.inqool.arclib.dto.StoredFileInfoDto;
import cz.inqool.arclib.service.AipRef;
import cz.inqool.arclib.service.ArchivalDbService;
import cz.inqool.arclib.service.ArchivalService;
import cz.inqool.arclib.service.FileRef;
import cz.inqool.uas.exception.BadArgument;
import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/storage/aip")
public class AipApi {

    private ArchivalService archivalService;
    private ArchivalDbService archivalDbService;


    /**
     * Retrieves specified AIP as a ZIP package containing AipSip and all AipXmls.
     *
     * @param sipId unique AIP identifier
     */
    @RequestMapping(value = "/{sipId}", method = RequestMethod.GET)
    public void get(@PathVariable("sipId") String sipId, HttpServletResponse response) throws IOException {
        AipRef aip = archivalService.get(sipId);
        response.setContentType("application/zip");
        response.setStatus(200);
        response.addHeader("Content-Disposition", "attachment; filename=aip" + aip.getSip().getId());

        try (ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(response.getOutputStream()))) {
            zipOut.putNextEntry(new ZipEntry(aip.getSip().getName()));
            IOUtils.copyLarge(new BufferedInputStream(aip.getSip().getStream()), zipOut);
            zipOut.closeEntry();
            IOUtils.closeQuietly(aip.getSip().getStream());
            for (FileRef xml : aip.getXmls()) {
                zipOut.putNextEntry(new ZipEntry(xml.getName()));
                IOUtils.copyLarge(new BufferedInputStream(xml.getStream()), zipOut);
                zipOut.closeEntry();
                IOUtils.closeQuietly(xml.getStream());
            }
        }
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
     * @return Information about both stored files containing file name, its assigned id and boolean flag determining whether the stored file is consistent i.e. was not changed during the transfer.
     */
    @RequestMapping(value = "/store", method = RequestMethod.POST)
    public List<StoredFileInfoDto> save(@RequestParam("sip") MultipartFile sip, @RequestParam("xml") MultipartFile xml, @RequestParam("meta") MultipartFile meta) throws IOException {
        try (InputStream sipStream = sip.getInputStream();
             InputStream xmlStream = xml.getInputStream();
             InputStream metaStream = meta.getInputStream()) {
            return archivalService.store(sipStream, sip.getOriginalFilename(), xmlStream, xml.getOriginalFilename(), metaStream);
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
     * @param meta  File containing one line with ARCLib XML MD5 hash
     * @return Information about stored xml containing file name, its assigned id and boolean flag determining whether the stored file is consistent i.e. was not changed during the transfer.
     */
    @RequestMapping(value = "/{sipId}/update", method = RequestMethod.POST)
    public StoredFileInfoDto updateXml(@PathVariable("sipId") String sipId, @RequestParam("xml") MultipartFile xml, @RequestParam("meta") MultipartFile meta) {
        try (InputStream xmlStream = xml.getInputStream();
             InputStream metaStream = meta.getInputStream()) {
            return archivalService.updateXml(sipId, xml.getOriginalFilename(), xmlStream, metaStream);
        } catch (IOException e) {
            throw new BadArgument(e);
        }
    }

    /**
     * Logically removes AIP package by setting its state to <i><removed/i>.
     *
     * @param sipId
     */
    @RequestMapping(value = "/{sipId}", method = RequestMethod.DELETE)
    public void remove(@PathVariable("sipId") String sipId) {
        archivalDbService.removeSip(sipId);
    }

    /**
     * Physically removes SIP part of AIP package and setts its state to <i><deleted/i>. XMLs and data in transaction database are not removed.
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
        return archivalDbService.getAip(uuid).getState();
    }


    /**
     * Retrieves state of Archival Storage.
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

    @Inject
    public void setArchivalDbService(ArchivalDbService archivalDbService) {
        this.archivalDbService = archivalDbService;
    }
}
