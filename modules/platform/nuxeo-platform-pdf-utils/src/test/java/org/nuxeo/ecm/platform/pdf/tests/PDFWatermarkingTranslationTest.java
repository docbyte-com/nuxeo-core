/*
 * (C) Copyright 2016-2024 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thibaud Arguillere
 *     Miguel Nixo
 *     Michael Vachette
 */
package org.nuxeo.ecm.platform.pdf.tests;

import java.awt.geom.Point2D;

import jakarta.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.platform.pdf.service.PDFTransformationServiceImpl;
import org.nuxeo.ecm.platform.pdf.service.watermark.WatermarkProperties;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@Deploy("org.nuxeo.ecm.platform.pdf")
public class PDFWatermarkingTranslationTest {

    protected static long PAGE_WIDTH = 400;

    protected static long PAGE_HEIGHT = 800;

    protected static long WATERMARK_WIDTH = 200;

    protected static long WATERMARK_HEIGHT = 100;

    @Inject
    protected PDFTransformationServiceImpl pdfTransformationService;

    @Test
    public void testBottomLeftCorner() {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        Point2D vector = pdfTransformationService.computeTranslationVector(PAGE_WIDTH, WATERMARK_WIDTH, PAGE_HEIGHT,
                WATERMARK_HEIGHT, properties);
        Assert.assertEquals(0, vector.getX(), 1L);
        Assert.assertEquals(0, vector.getY(), 1L);
    }

    @Test
    public void testBottomRightCorner() {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setInvertX(true);
        Point2D vector = pdfTransformationService.computeTranslationVector(PAGE_WIDTH, WATERMARK_WIDTH, PAGE_HEIGHT,
                WATERMARK_HEIGHT, properties);
        Assert.assertEquals(PAGE_WIDTH - WATERMARK_WIDTH, vector.getX(), 1L);
        Assert.assertEquals(0, vector.getY(), 1L);
    }

    @Test
    public void testTopRightCorner() {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setInvertX(true);
        properties.setInvertY(true);
        Point2D vector = pdfTransformationService.computeTranslationVector(PAGE_WIDTH, WATERMARK_WIDTH, PAGE_HEIGHT,
                WATERMARK_HEIGHT, properties);
        Assert.assertEquals(PAGE_WIDTH - WATERMARK_WIDTH, vector.getX(), 1L);
        Assert.assertEquals(PAGE_HEIGHT - WATERMARK_HEIGHT, vector.getY(), 1L);
    }

    @Test
    public void testTopRightCornerWithMargin() {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setxPosition(50);
        properties.setyPosition(50);
        properties.setInvertX(true);
        properties.setInvertY(true);
        Point2D vector = pdfTransformationService.computeTranslationVector(PAGE_WIDTH, WATERMARK_WIDTH, PAGE_HEIGHT,
                WATERMARK_HEIGHT, properties);
        Assert.assertEquals(PAGE_WIDTH - WATERMARK_WIDTH - 50, vector.getX(), 1L);
        Assert.assertEquals(PAGE_HEIGHT - WATERMARK_HEIGHT - 50, vector.getY(), 1L);
    }

    @Test
    public void testTopLeftCorner() {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setInvertY(true);
        Point2D vector = pdfTransformationService.computeTranslationVector(PAGE_WIDTH, WATERMARK_WIDTH, PAGE_HEIGHT,
                WATERMARK_HEIGHT, properties);
        Assert.assertEquals(0, vector.getX(), 1L);
        Assert.assertEquals(PAGE_HEIGHT - WATERMARK_HEIGHT, vector.getY(), 1L);
    }

    @Test
    public void testCenter() {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setRelativeCoordinates(true);
        properties.setxPosition(0.5);
        properties.setyPosition(0.5);
        Point2D vector = pdfTransformationService.computeTranslationVector(PAGE_WIDTH, WATERMARK_WIDTH, PAGE_HEIGHT,
                WATERMARK_HEIGHT, properties);
        Assert.assertEquals((PAGE_WIDTH - WATERMARK_WIDTH) / 2, vector.getX(), 1L);
        Assert.assertEquals((PAGE_HEIGHT - WATERMARK_HEIGHT) / 2, vector.getY(), 1L);
    }

    @Test
    public void testBottomLeftCornerRotationDown() {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setRotation(-90);
        Point2D vector = pdfTransformationService.computeTranslationVector(PAGE_WIDTH, WATERMARK_WIDTH, PAGE_HEIGHT,
                WATERMARK_HEIGHT, properties);
        Assert.assertEquals(0, vector.getX(), 1L);
        Assert.assertEquals(WATERMARK_WIDTH, vector.getY(), 1L);
    }

    @Test
    public void testBottomLeftCornerRotationDownWithMargin() {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setRotation(-90);
        properties.setxPosition(50);
        properties.setyPosition(50);
        Point2D vector = pdfTransformationService.computeTranslationVector(PAGE_WIDTH, WATERMARK_WIDTH, PAGE_HEIGHT,
                WATERMARK_HEIGHT, properties);
        Assert.assertEquals(50, vector.getX(), 1L);
        Assert.assertEquals(WATERMARK_WIDTH + 50, vector.getY(), 1L);
    }

    @Test
    public void testBottomLeftCornerRotationUp() {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setRotation(90);
        Point2D vector = pdfTransformationService.computeTranslationVector(PAGE_WIDTH, WATERMARK_WIDTH, PAGE_HEIGHT,
                WATERMARK_HEIGHT, properties);
        Assert.assertEquals(WATERMARK_HEIGHT, vector.getX(), 1L);
        Assert.assertEquals(0, vector.getY(), 1L);
    }

    @Test
    public void testTopRightCornerRotationDown() {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setInvertX(true);
        properties.setInvertY(true);
        properties.setRotation(-90);
        Point2D vector = pdfTransformationService.computeTranslationVector(PAGE_WIDTH, WATERMARK_WIDTH, PAGE_HEIGHT,
                WATERMARK_HEIGHT, properties);
        Assert.assertEquals(PAGE_WIDTH - WATERMARK_HEIGHT, vector.getX(), 1L);
        Assert.assertEquals(PAGE_HEIGHT, vector.getY(), 1L);
    }

    @Test
    public void testTopRightCornerRotationUp() {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setInvertX(true);
        properties.setInvertY(true);
        properties.setRotation(90);
        Point2D vector = pdfTransformationService.computeTranslationVector(PAGE_WIDTH, WATERMARK_WIDTH, PAGE_HEIGHT,
                WATERMARK_HEIGHT, properties);
        Assert.assertEquals(PAGE_WIDTH, vector.getX(), 1L);
        Assert.assertEquals(PAGE_HEIGHT - WATERMARK_WIDTH, vector.getY(), 1L);
    }

    @Test
    public void testTopRightCornerRotationUpWithMargin() {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setInvertX(true);
        properties.setInvertY(true);
        properties.setxPosition(50);
        properties.setyPosition(50);
        properties.setRotation(90);
        Point2D vector = pdfTransformationService.computeTranslationVector(PAGE_WIDTH, WATERMARK_WIDTH, PAGE_HEIGHT,
                WATERMARK_HEIGHT, properties);
        Assert.assertEquals(PAGE_WIDTH - 50, vector.getX(), 1L);
        Assert.assertEquals(PAGE_HEIGHT - WATERMARK_WIDTH - 50, vector.getY(), 1L);
    }

    @Test
    public void testCenterRotationUp() {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setRelativeCoordinates(true);
        properties.setxPosition(0.5);
        properties.setyPosition(0.5);
        properties.setRotation(90);
        Point2D vector = pdfTransformationService.computeTranslationVector(PAGE_WIDTH, WATERMARK_WIDTH, PAGE_HEIGHT,
                WATERMARK_HEIGHT, properties);
        Assert.assertEquals((PAGE_WIDTH + WATERMARK_HEIGHT) / 2, vector.getX(), 1L);
        Assert.assertEquals(PAGE_HEIGHT / 2 - WATERMARK_WIDTH / 2, vector.getY(), 1L);
    }

    @Test
    public void testCenterRotationDown() {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setRelativeCoordinates(true);
        properties.setxPosition(0.5);
        properties.setyPosition(0.5);
        properties.setRotation(-90);
        Point2D vector = pdfTransformationService.computeTranslationVector(PAGE_WIDTH, WATERMARK_WIDTH, PAGE_HEIGHT,
                WATERMARK_HEIGHT, properties);
        Assert.assertEquals((PAGE_WIDTH - WATERMARK_HEIGHT) / 2, vector.getX(), 1L);
        Assert.assertEquals(PAGE_HEIGHT / 2 + WATERMARK_WIDTH / 2, vector.getY(), 1L);
    }

}
