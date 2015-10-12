/*
 * (c) Copyright 2015 freiheit.com technologies GmbH
 *
 * Created on 17.08.2015 by florian.diebold@freiheit.com
 *
 * This file contains unpublished, proprietary trade secret information of
 * freiheit.com technologies GmbH. Use, transcription, duplication and
 * modification are strictly prohibited without prior written consent of
 * freiheit.com technologies GmbH.
 */
package com.freiheit.fuava.sftp;

import com.freiheit.fuava.sftp.util.FilenameUtil;
import com.freiheit.fuava.sftp.util.FileType;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test for FilenameUtil.
 *
 * @author florian.diebold@freiheit.com
 */
public class FilenameUtilTest {
    /**
     * Test for matchesSearchedFile.
     */
    @Test( dataProvider = "fileTypes" )
    public void testMatchesSearchedFile( final FileType fileType, final String fileName, final boolean shouldMatch ) {
        Assert.assertEquals( FilenameUtil.matchesSearchedFile( fileName, fileType, null, null ), shouldMatch );
    }

    @DataProvider( name = "fileTypes" )
    public Object[][] getFileTypesData() {
        final FileType pwhg = new FileType( "pwhg", "_ho_pwhg_dat{1}" );
        final FileType mcrm = new FileType( "abc", "ABC_ABC_Data" );
        return new Object[][] {
                { pwhg, "de_dev_ho_pwhg_data_20151122_120000.csv", true },
                { pwhg, "de_dev_ho_pwhg_data_0-2_20151121_120000.csv", true },
                { pwhg, "fr_pp_ho_pwhg_data_0-2_20181231_163759.csv", true },
                { pwhg, "de_dev_st_art_dat_15_all_data_0-2_20151121_120000.csv", false },
                { mcrm, "ABC_ABC_Data_152000_20150812_100000.csv", true },
        };
    }
}

