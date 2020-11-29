/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.levitator.oath_wallet_service.tests;

import com.levitator.oath_wallet_service.util.SimpleGlobMatcher;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

//
// This glob system is really simple. The match pattern system essentially amounts
// to an asterisk-delimited list of tokens which must occur in order and are permitted
// to have anything in between.
//
// Security concerns:
// Say we specify a glob of the form: https://*.google.com/*
// A malicious website could spoof that glob with this URL: https://www.scamtown.com/.google.com/virus.exe
//
// So, we need to either make this risk clear in the documentation, or maybe add a software trap that warns
// when you enter a glob that is host-ambiguous. The obvious solution is not to permit any wildcards within
// the host portion of the URL. This is mitigated by the fact that you have to delibertely hit 
// the PIN hotkey in the browser in order to transmit a PIN number, and if you have sense, then you probably have your
// smartcard configured to demand touch activation and/or PIN entry, in addition.
//
public class TestSimpleGlobber {
    
    static SimpleGlobMatcher matcher1, matcher2, matcher3;
    
    public TestSimpleGlobber() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        try{
            matcher1 = new SimpleGlobMatcher("https://localhost/*");
            matcher2 = new SimpleGlobMatcher("https://*.protonmail.com/*");
            matcher3 = new SimpleGlobMatcher("aaa*abc*abc*abczzz"); //aaaabcabcabcabcabczzzabczzz
        }
        catch(Exception ex){
            throw new RuntimeException("FAIL", ex);
        }
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
                                         
    @Test
    public void match1() {
        //Trailing star match
        assert( matcher1.match( "https://localhost/abcdefg") );
    }
    
    @Test
    public void match2() {
        //Missing last character of last substring-match
        assert( !matcher2.match( "https://abc.protonmail.com" ) );
    }
    
    @Test
    //Blank star match in the middle and end
    public void match3() {
        assert( matcher2.match( "https://.protonmail.com/" ) );
    }
    
    @Test
    //Non blank star matches in middle and end
    public void match4() {
        assert( matcher2.match( "https://abc.protonmail.com/abc/asdfsd" ) );
    }
    
    @Test
    public void match() {
        assert( matcher3.match( "aaaabcabcabcabcabczzzabczzz" ) );
    }
}
