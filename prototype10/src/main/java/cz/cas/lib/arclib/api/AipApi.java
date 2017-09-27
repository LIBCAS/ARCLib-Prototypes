package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.AipSip;
import cz.cas.lib.arclib.dto.StorageStateDto;
import cz.cas.lib.arclib.exception.BadArgument;
import cz.cas.lib.arclib.exception.ChecksumChanged;
import cz.cas.lib.arclib.exception.NotFound;
import cz.cas.lib.arclib.service.AipRef;
import cz.cas.lib.arclib.service.ArchivalService;
import cz.cas.lib.arclib.service.FileRef;
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
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static cz.cas.lib.arclib.util.Utils.checked;

@RestController
@RequestMapping("/api/storage")
public class AipApi {

    private ArchivalService archivalService;

    /**
     * Retrieves specified AIP as ZIP package.
     * @param sipId
     * @param all   if true all AIP XMLs are packed otherwise only the latest is packed
     * @throws IOException
     * @throws NotFound if AIP does not exist or is in {@link cz.cas.lib.arclib.domain.AipState#DELETED} state
     */
    @RequestMapping(value = "/{sipId}", method = RequestMethod.GET)
    public void get(@PathVariable("sipId") String sipId, @RequestParam(value = "all") Optional<Boolean> all,  HttpServletResponse response) throws IOException, NotFound {
        checkUUID(sipId);

        AipRef aip = archivalService.get(sipId,all);
        response.setContentType("application/zip");
        response.setStatus(200);
        response.addHeader("Content-Disposition", "attachment; filename=aip_" + aip.getSip().getId());

        try (ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(response.getOutputStream()))) {
            zipOut.putNextEntry(new ZipEntry(aip.getSip().getId()));
            IOUtils.copyLarge(new BufferedInputStream(aip.getSip().getStream()), zipOut);
            zipOut.closeEntry();
            IOUtils.closeQuietly(aip.getSip().getStream());
            for (FileRef xml : aip.getXmls()) {
                zipOut.putNextEntry(new ZipEntry(String.format("%s_xml_%d",sipId,xml.getVersion())));
                IOUtils.copyLarge(new BufferedInputStream(xml.getStream()), zipOut);
                zipOut.closeEntry();
                IOUtils.closeQuietly(xml.getStream());
            }
        }
    }

    /**
     * Retrieves specified AIP XML file.
     * @param sipId
     * @param version   version of XML, if not specified the latest version is retrieved
     * @throws IOException
     * @throws NotFound if either SIP or specified version of XML does not exist
     */
    @RequestMapping(value = "/xml/{sipId}", method = RequestMethod.GET)
    public void getXml(@PathVariable("sipId") String sipId, @RequestParam(value = "v") Optional<Integer> version,HttpServletResponse response) throws IOException, NotFound {
        checkUUID(sipId);
        FileRef xml = archivalService.getXml(sipId,version);
        response.setContentType("application/xml");
        response.setStatus(200);
        response.addHeader("Content-Disposition", "attachment; filename=" + sipId + "_xml_"+xml.getVersion());
        IOUtils.copyLarge(xml.getStream(),response.getOutputStream());
        IOUtils.closeQuietly(xml.getStream());
    }

    /**
     * Stores AIP parts (SIP and ARCLib XML) into Archival Storage.
     * <p>
     * Verifies that data are consistent after transfer and if not storage and database are cleared.
     * </p>
     * <p>
     * This endpoint also handles AIP versioning when whole AIP is versioned.
     * </p>
     * @param sip    SIP part of AIP
     * @param xml    ARCLib XML part of AIP
     * @param sipMD5 SIP md5 hash
     * @param xmlMD5 XML md5 hash
     * @param id    optional parameter, if not specified id is generated
     * @return SIP ID of created AIP
     * @throws IOException
     * @throws ChecksumChanged  if provided MD5 checksums does not match checksums of stored files
     */
    @RequestMapping(value = "/store", method = RequestMethod.POST)
    public String save(@RequestParam("sip") MultipartFile sip, @RequestParam("xml") MultipartFile xml, @RequestParam("sipMD5") String sipMD5, @RequestParam("xmlMD5") String xmlMD5, @RequestParam(value = "UUID") Optional<String> id) throws IOException, ChecksumChanged {
        checkMD5(xmlMD5);
        checkMD5(sipMD5);
        try (InputStream sipStream = sip.getInputStream();
             InputStream xmlStream = xml.getInputStream()) {
            return archivalService.store(sipStream, sipMD5, xmlStream, xmlMD5,id);
        }
    }

    /**
     * Stores ARCLib AIP XML into Archival Storage.
     * <p>
     * Verifies that data are consistent after transfer and if not storage and database are cleared.
     * </p>
     * <p>
     * This endpoint handles AIP versioning when AIP XML is versioned.
     * </p>
     * @param sipId  Id of SIP to which XML belongs
     * @param xml    ARCLib XML
     * @param xmlMD5 XML md5 hash
     * @throws ChecksumChanged  if provided MD5 checksum does not match checksum of stored XML
     */
    @RequestMapping(value = "/{sipId}/update", method = RequestMethod.POST)
    public void updateXml(@PathVariable("sipId") String sipId, @RequestParam("xml") MultipartFile xml, @RequestParam("xmlMD5") String xmlMD5) throws ChecksumChanged {
        checkUUID(sipId);
        checkMD5(xmlMD5);
        try (InputStream xmlStream = xml.getInputStream()) {
            archivalService.updateXml(sipId, xmlStream, xmlMD5);
        } catch (IOException e) {
            throw new BadArgument(e);
        }
    }

    /**
     * Logically removes AIP package by setting its state to {@link cz.cas.lib.arclib.domain.AipState#REMOVED}
     * <p>Removed package can is still retrieved when {@link AipApi#get} method is called.</p>
     * @param sipId
     */
    @RequestMapping(value = "/{sipId}", method = RequestMethod.DELETE)
    public void remove(@PathVariable("sipId") String sipId) throws IOException {
        checkUUID(sipId);
        archivalService.remove(sipId);
    }

    /**
     * Physically removes SIP part of AIP package and setts its state to {@link cz.cas.lib.arclib.domain.AipState#DELETED}. XMLs and data in transaction database are not removed.
     * <p>Deleted package is no longer retrieved when {@link AipApi#get} method is called.</p>
     * @param sipId
     */
    @RequestMapping(value = "/{sipId}/hard", method = RequestMethod.DELETE)
    public void delete(@PathVariable("sipId") String sipId) throws IOException {
        checkUUID(sipId);
        archivalService.delete(sipId);
    }

    /**
     * Retrieves information about AIP containing state, id, XMLs ...
     *
     * @param sipId
     * @return
     */
    @RequestMapping(value = "/{uuid}/state", method = RequestMethod.GET)
    public AipSip getAipInfo(@PathVariable("uuid") String sipId) throws IOException {
        checkUUID(sipId);
        return archivalService.getAipInfo(sipId);
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

    private void checkUUID(String id) {
        checked(() -> UUID.fromString(id), () -> new BadArgument(id));
    }

    private void checkMD5(String s) {
        if(!s.matches("\\p{XDigit}{32}"))
            throw new BadArgument(s);
    }

    @Inject
    public void setArchivalService(ArchivalService archivalService) {
        this.archivalService = archivalService;
    }
}
