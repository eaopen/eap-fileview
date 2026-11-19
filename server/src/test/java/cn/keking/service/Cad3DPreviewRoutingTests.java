package cn.keking.service;

import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import cn.keking.service.impl.CommonPreviewImpl;
import cn.keking.service.impl.Online3DFilePreviewImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class Cad3DPreviewRoutingTests {

    @Test
    void established2dCadSuffixesUseCadConverter() {
        for (String filename : List.of("drawing.dwg", "drawing.DXF", "drawing.dwf", "drawing.cf2")) {
            assertEquals(FileType.CAD, FileType.typeFromFileName(filename), filename);
        }
    }

    @Test
    void existing3dRoutesRemainStableIncludingOverlappingSuffixes() {
        for (String filename : List.of("mesh.stl", "assembly.ifc", "part.iges", "model.step",
                "model.stp", "model.glb", "scene.gltf", "geometry.brep")) {
            assertEquals(FileType.ONLINE3D, FileType.typeFromFileName(filename), filename);
        }
    }

    @Test
    void nativeProprietaryCadFormatsAreProjectExtensionsNotBuiltInClaims() {
        for (String filename : List.of("design.prt", "drawing.dft", "assembly.jt",
                "part.par", "part.psm", "assembly.asm", "piece.sldprt")) {
            assertEquals(FileType.OTHER, FileType.typeFromFileName(filename), filename);
        }
    }

    @Test
    void projectProviderHandlesNativeFormatWithoutTakingOverGeneric3d() {
        ApplicationContext context = mock(ApplicationContext.class);
        FilePreviewFactory factory = new FilePreviewFactory(context);
        FilePreviewProvider nativeProvider = new FilePreviewProvider() {
            @Override
            public boolean supports(FileAttribute file) {
                return file.getType() == FileType.OTHER
                        && "prt".equalsIgnoreCase(file.getSuffix());
            }
            @Override
            public String filePreviewHandle(String url, Model model, FileAttribute file) {
                return "pdf";
            }
        };
        factory.registerExtensions(List.of(nativeProvider));
        FileAttribute nx = new FileAttribute(FileType.OTHER, "PRT", "drawing.prt",
                "https://files.example/drawing.prt");
        assertSame(nativeProvider, factory.get(nx));

        FileAttribute glb = new FileAttribute(FileType.ONLINE3D, "glb", "model.glb",
                "https://files.example/model.glb");
        FilePreview default3d = mock(FilePreview.class);
        when(context.getBean(FileType.ONLINE3D.getInstanceName(), FilePreview.class))
                .thenReturn(default3d);
        assertSame(default3d, factory.get(glb));
    }

    @Test
    void failed3dSourcePreparationShowsErrorPageNotEmptyViewer() {
        CommonPreviewImpl common = mock(CommonPreviewImpl.class);
        FileAttribute file = new FileAttribute(FileType.ONLINE3D, "glb",
                "part.glb", "https://files.example/part.glb");
        when(common.filePreviewHandle(eq("https://files.example/part.glb"), any(), eq(file)))
                .thenReturn(FilePreview.NOT_SUPPORTED_FILE_PAGE);
        assertEquals(FilePreview.NOT_SUPPORTED_FILE_PAGE,
                new Online3DFilePreviewImpl(common).filePreviewHandle(
                        "https://files.example/part.glb", new ExtendedModelMap(), file));
    }

    @Test
    void valid3dSourceRendersOriginalViewer() {
        CommonPreviewImpl common = mock(CommonPreviewImpl.class);
        FileAttribute file = new FileAttribute(FileType.ONLINE3D, "glb",
                "part.glb", "https://files.example/part.glb");
        when(common.filePreviewHandle(eq("https://files.example/part.glb"), any(), eq(file)))
                .thenReturn(null);
        assertEquals(FilePreview.ONLINE3D_PREVIEW_PAGE,
                new Online3DFilePreviewImpl(common).filePreviewHandle(
                        "https://files.example/part.glb", new ExtendedModelMap(), file));
    }
}
