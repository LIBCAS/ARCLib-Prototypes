package cz.inqool.arclib.api;

import cz.inqool.arclib.domain.AipSip;
import cz.inqool.arclib.dto.StorageStateDto;
import cz.inqool.arclib.dto.StoredFileInfoDto;
import cz.inqool.arclib.exception.BadArgument;
import cz.inqool.arclib.service.AipRef;
import cz.inqool.arclib.service.ArchivalDbService;
import cz.inqool.arclib.service.ArchivalService;
import cz.inqool.arclib.service.FileRef;
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
@RequestMapping("/api/storage")
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
        response.addHeader("Content-Disposition", "attachment; filename=aip_" + aip.getSip().getId());

        try (ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(response.getOutputStream()))) {
            zipOut.putNextEntry(new ZipEntry(aip.getSip().getId() + "_" + aip.getSip().getName()));
            IOUtils.copyLarge(new BufferedInputStream(aip.getSip().getStream()), zipOut);
            zipOut.closeEntry();
            IOUtils.closeQuietly(aip.getSip().getStream());
            for (FileRef xml : aip.getXmls()) {
                zipOut.putNextEntry(new ZipEntry(xml.getId() + "_" + xml.getName()));
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
     * @param sip    SIP part of AIP
     * @param xml    ARCLib XML part of AIP
     * @param sipMD5 SIP md5 hash
     * @param xmlMD5 XML md5 hash
     * @return Information about both stored files containing file name, its assigned id and boolean flag determining whether the stored file is consistent i.e. was not changed during the transfer.
     */
    @RequestMapping(value = "/store", method = RequestMethod.POST)
    public List<StoredFileInfoDto> save(@RequestParam("sip") MultipartFile sip, @RequestParam("xml") MultipartFile xml, @RequestParam("sipMD5") String sipMD5, @RequestParam("xmlMD5") String xmlMD5) throws IOException {
        try (InputStream sipStream = sip.getInputStream();
             InputStream xmlStream = xml.getInputStream()) {
            return archivalService.store(sipStream, sip.getOriginalFilename(), sipMD5, xmlStream, xml.getOriginalFilename(), xmlMD5);
        }
    }

    /**
     * Stores ARCLib AIP XML into Archival Storage.
     * <p>
     * This endpoint handles AIP versioning when AIP XML is versioned.
     * </p>
     *
     * @param sipId  Id of SIP to which XML belongs
     * @param xml    ARCLib XML
     * @param xmlMD5 XML md5 hash
     * @return Information about stored xml containing file name, its assigned id and boolean flag determining whether the stored file is consistent i.e. was not changed during the transfer.
     */
    @RequestMapping(value = "/{sipId}/update", method = RequestMethod.POST)
    public StoredFileInfoDto updateXml(@PathVariable("sipId") String sipId, @RequestParam("xml") MultipartFile xml, @RequestParam("xmlMD5") String xmlMD5) {
        try (InputStream xmlStream = xml.getInputStream()) {
            return archivalService.updateXml(sipId, xml.getOriginalFilename(), xmlStream, xmlMD5);
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
     * Retrieves information about AIP.
     *
     * @param uuid
     * @return
     */
    @RequestMapping(value = "/{uuid}/state", method = RequestMethod.GET)
    public AipSip getAipInfo(@PathVariable("uuid") String uuid) throws IOException {
        return archivalService.getAipInfo(uuid);
    }


    /**
     * Retrieves state of Archival Storage.
     *
     * @return
     */
    @RequestMapping(value = "/state", method = RequestMethod.GET)
    public StorageStateDto getStorageState() {
        return archivalService.getStorageState();
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
