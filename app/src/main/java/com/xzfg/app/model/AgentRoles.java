package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

@SuppressWarnings("unused")
public class AgentRoles implements Parcelable {

  private int role_alert = 0;
  private int role_alertacceptance = 0;
  private int role_alertspreference = 0;
  private int role_audiorecord = 0;
  private int role_audiostream = 0;
  private int role_bossmode = 0;
  private int role_bossmodeblack = 0;
  private int role_bossmodepref = 0;
  private int role_broadcastingservice = 0;
  private int role_buttonemergency = 0;
  private int role_buttonnonemergency = 0;
  private int role_buttonpanic = 0;
  private int role_cases = 0;
  private int role_chat = 0;
  private int role_checkin = 0;
  private int role_collectedmedia = 0;
  private int role_contacts = 0;
  private int role_defaultscreen = 0;
  private int role_existingmedia = 0;
  private int role_exitbossmode = 1;
  private int role_factoryreset = 0;
  private int role_help = 0;
  private int role_home = 1;
  private int role_home2 = 0;
  private int role_livetracking = 0;
  private int role_livetrackingon = 0;
  private int role_map = 0;
  private int role_mapcapture = 0;
  private int role_overview = 0;
  private int role_panic = 0;
  private int role_paniccovert = 0;
  private int role_paniccovertheadphones = 0;
  private int role_pointsofinterest = 0;
  private int role_panicovert = 0;
  private int role_panictimed = 0;
  private int role_panicvolumebuttons = 0;
  private int role_phonecollect = 0;
  private int role_phonelog = 0;
  private int role_phonerecord = 0;
  private int role_photocollect = 0;
  private int role_photos = 0;
  private int role_security = 0;
  private int role_smslog = 0;
  private int role_SMS_Fixes = 0;
  private int role_SMS_SOS = 0;
  private int role_task = 0;
  private int role_videocases = 0;
  private int role_videorecord = 0;
  private int role_videostream = 0;
  private int role_videostreaming = 0;
  private int role_wipedata = 0;
  private int role_g4_status = 0;
  private int role_g4_mark_alert =0;
  private int role_g4_configuration = 0;
  private int role_g4_messenger = 0;
  private int role_g4_at_commands = 0;
  public AgentRoles() {
  }

  public AgentRoles(AgentRoles agentRoles) {
    this.role_audiorecord = agentRoles.role_audiorecord;
    this.role_audiostream = agentRoles.role_audiostream;
    this.role_existingmedia = agentRoles.role_existingmedia;
    this.role_photocollect = agentRoles.role_photocollect;
    this.role_phonecollect = agentRoles.role_phonecollect;
    this.role_videorecord = agentRoles.role_videorecord;
    this.role_videostream = agentRoles.role_videostream;
    this.role_alert = agentRoles.role_alert;
    this.role_bossmode = agentRoles.role_bossmode;
    this.role_bossmodeblack = agentRoles.role_bossmodeblack;
    this.role_home = agentRoles.role_home;
    this.role_chat = agentRoles.role_chat;
    this.role_collectedmedia = agentRoles.role_collectedmedia;
    this.role_map = agentRoles.role_map;
    this.role_paniccovert = agentRoles.role_paniccovert;
    this.role_paniccovertheadphones = agentRoles.role_paniccovertheadphones;
    this.role_panicovert = agentRoles.role_panicovert;
    this.role_pointsofinterest = agentRoles.role_pointsofinterest;
    this.role_wipedata = agentRoles.role_wipedata;
    this.role_bossmodepref = agentRoles.role_bossmodepref;
    this.role_cases = agentRoles.role_cases;
    this.role_defaultscreen = agentRoles.role_defaultscreen;
    this.role_livetracking = agentRoles.role_livetracking;
    this.role_photos = agentRoles.role_photos;
    this.role_security = agentRoles.role_security;
    this.role_SMS_Fixes = agentRoles.role_SMS_Fixes;
    this.role_SMS_SOS = agentRoles.role_SMS_SOS;
    this.role_videocases = agentRoles.role_videocases;
    this.role_videostreaming = agentRoles.role_videostreaming;
    this.role_broadcastingservice = agentRoles.role_broadcastingservice;
    this.role_help = agentRoles.role_help;
    this.role_exitbossmode = agentRoles.role_exitbossmode;
    this.role_livetrackingon = agentRoles.role_livetrackingon;
    // v3.0
    this.role_alertacceptance = agentRoles.role_alertacceptance;
    this.role_factoryreset = agentRoles.role_factoryreset;
    this.role_contacts = agentRoles.role_contacts;
    this.role_checkin = agentRoles.role_checkin;
    this.role_panictimed = agentRoles.role_panictimed;
    this.role_mapcapture = agentRoles.role_mapcapture;
    this.role_panic = agentRoles.role_panic;
    this.role_alertspreference = agentRoles.role_alertspreference;
    this.role_smslog = agentRoles.role_smslog;
    this.role_phonerecord = agentRoles.role_phonerecord;
    this.role_phonelog = agentRoles.role_phonelog;
    // added 12/27/2016
    this.role_panicvolumebuttons = agentRoles.role_panicvolumebuttons;
    this.role_task = agentRoles.role_task;
    this.role_home2 = agentRoles.role_home2;
    this.role_overview = agentRoles.role_overview;
    // add 07/08/2017
    this.role_g4_at_commands = agentRoles.role_g4_at_commands;
    this.role_g4_mark_alert = agentRoles.role_g4_mark_alert;
    this.role_g4_configuration = agentRoles.role_g4_configuration;
    this.role_g4_messenger = agentRoles.role_g4_messenger;
    this.role_g4_status = agentRoles.role_g4_status;
  }

  public boolean audiorecord() {
    return role_audiorecord == 1;
  }

  public void setRole_audiorecord(int role_audiorecord) {
    this.role_audiorecord = role_audiorecord;
  }

  public boolean audiostream() {
    return role_audiostream == 1;
  }

  public void setRole_audiostream(int role_audiostream) {
    this.role_audiostream = role_audiostream;
  }

  public boolean existingmedia() {
    return role_existingmedia == 1;
  }

  public void setRole_existingmedia(int role_existingmedia) {
    this.role_existingmedia = role_existingmedia;
  }

  public boolean photocollect() {
    return role_photocollect == 1;
  }

  public void setRole_photocollect(int role_photocollect) {
    this.role_photocollect = role_photocollect;
  }

  public boolean phonecollect() {
    return role_phonecollect == 1;
  }

  public void setRole_phonecollect(int role_phonecollect) {
    this.role_phonecollect = role_phonecollect;
  }

  public boolean videorecord() {
    return role_videorecord == 1;
  }

  public void setRole_videorecord(int role_videorecord) {
    this.role_videorecord = role_videorecord;
  }

  public boolean videostream() {
    return role_videostream == 1;
  }

  public void setRole_videostream(int role_videostream) {
    this.role_videostream = role_videostream;
  }

  public boolean alert() {
    return role_alert == 1;
  }

  public void setRole_alert(int role_alert) {
    this.role_alert = role_alert;
  }

  public boolean bossmode() {
    return role_bossmode == 1;
  }

  public void setRole_bossmode(int role_bossmode) {
    this.role_bossmode = role_bossmode;
  }

  public boolean bossmodeblack() {
    return role_bossmodeblack == 1;
  }

  public void setRole_bossmodeblack(int role_bossmodeblack) {
    this.role_bossmodeblack = role_bossmodeblack;
  }

  public boolean chat() {
    return role_chat == 1;
  }

  public void setRole_chat(int role_chat) {
    this.role_chat = role_chat;
  }

  public boolean home() {
    return role_home == 1;
  }

  public void setRole_home(int role_home) {
    this.role_home = role_home;
  }

  public boolean collectedmedia() {
    return role_collectedmedia == 1;
  }

  public void setRole_collectedmedia(int role_collectedmedia) {
    this.role_collectedmedia = role_collectedmedia;
  }

  public boolean map() {
    return role_map == 1;
  }

  public void setRole_map(int role_map) {
    this.role_map = role_map;
  }

  public boolean paniccovert() {
    return role_paniccovert == 1;
  }

  public void setRole_paniccovert(int role_paniccovert) {
    this.role_paniccovert = role_paniccovert;
  }

  public boolean pointsofinterest() {
    return role_pointsofinterest == 1;
  }

  public void setRole_pointsofinterest(int role_pointsofinterest) {
    this.role_pointsofinterest = role_pointsofinterest;
  }

  public boolean wipedata() {
    return role_wipedata == 1;
  }

  public void setRole_wipedata(int role_wipedata) {
    this.role_wipedata = role_wipedata;
  }

  public boolean paniccovertheadphones() {
    return role_paniccovertheadphones == 1;
  }

  public void setRole_paniccovertheadphones(int role_paniccovertheadphones) {
    this.role_paniccovertheadphones = role_paniccovertheadphones;
  }

  public boolean bossmodepref() {
    return role_bossmodepref == 1;
  }

  public void setRole_bossmodepref(int role_bossmodepref) {
    this.role_bossmodepref = role_bossmodepref;
  }

  public boolean cases() {
    return role_cases == 1;
  }

  public void setRole_cases(int role_cases) {
    this.role_cases = role_cases;
  }

  public boolean defaultscreen() {
    return role_defaultscreen == 1;
  }

  public void setRole_defaultscreen(int role_defaultscreen) {
    this.role_defaultscreen = role_defaultscreen;
  }

  public boolean livetracking() {
    return role_livetracking == 1;
  }

  public void setRole_livetracking(int role_livetracking) {
    this.role_livetracking = role_livetracking;
  }

  public boolean exitbossmode() {
    return role_exitbossmode == 1;
  }

  public void setRole_exitbossmode(int role_exitbossmode) {
    this.role_exitbossmode = role_exitbossmode;
  }

  public boolean livetrackingon() {
    return role_livetrackingon == 1;
  }

  public void setRole_livetrackingon(int role_livetrackingon) {
    this.role_livetrackingon = role_livetrackingon;
  }

  public boolean photos() {
    return role_photos == 1;
  }

  public void setRole_photos(int role_photos) {
    this.role_photos = role_photos;
  }

  public boolean security() {
    return role_security == 1;
  }

  public void setRole_security(int role_security) {
    this.role_security = role_security;
  }

  public boolean SMS_Fixes() {
    return role_SMS_Fixes == 1;
  }

  public void setRole_SMS_Fixes(int role_SMS_Fixes) {
    this.role_SMS_Fixes = role_SMS_Fixes;
  }

  public boolean SMS_SOS() {
    return role_SMS_SOS == 1;
  }

  public void setRole_SMS_SOS(int role_SMS_SOS) {
    this.role_SMS_SOS = role_SMS_SOS;
  }

  public void setRole_g4_status(int role_g4_status) {
      this.role_g4_status = role_g4_status;
  }
  public boolean g4_status(){
    return  role_g4_status == 1;
  }

  public void setRole_g4_configuration(int role_g4_configuration) {
      this.role_g4_configuration = role_g4_configuration;
  }
  public boolean g4_configuration(){
    return role_g4_configuration ==1;
  }
  public void setRole_g4_mark_alert(int role_g4_mark_alert) {
      this.role_g4_mark_alert = role_g4_mark_alert;
  }
  public boolean g4_mark_alert(){
    return role_g4_mark_alert==1;
  }
  public void setRole_g4_messenger(int role_g4_messenger) {
      this.role_g4_messenger = role_g4_messenger;
  }
  public boolean g4_messenger(){
    return role_g4_messenger == 1;
  }
  public void setRole_g4_at_commands(int role_g4_at_commands) {
      this.role_g4_at_commands = role_g4_at_commands;
  }
  public boolean g4_at_commands(){
    return role_g4_at_commands == 1;
  }

  // v3.0
  public boolean alertacceptance() {
    return role_alertacceptance == 1;
  }

  public void setRole_alertacceptance(int role_alertacceptance) {
    this.role_alertacceptance = role_alertacceptance;
  }

  public boolean factoryreset() {
    return role_factoryreset == 1;
  }

  public void setRole_factoryreset(int role_factoryreset) {
    this.role_factoryreset = role_factoryreset;
  }

  public boolean contacts() {
    return role_contacts == 1;
  }

  public void setRole_contacts(int role_contacts) {
    this.role_contacts = role_contacts;
  }

  public boolean checkin() {
    return role_checkin == 1;
  }

  public void setRole_checkin(int role_checkin) {
    this.role_checkin = role_checkin;
  }

  public boolean panictimed() {
    return role_panictimed == 1;
  }

  public void setRole_panictimed(int role_panictimed) {
    this.role_panictimed = role_panictimed;
  }

  public boolean mapcapture() {
    return role_mapcapture == 1;
  }

  public void setRole_mapcapture(int role_mapcapture) {
    this.role_mapcapture = role_mapcapture;
  }

  public boolean panic() {
    return role_panic == 1;
  }

  public void setRole_panic(int role_panic) {
    this.role_panic = role_panic;
  }

  public boolean alertspreference() {
    return role_alertspreference == 1;
  }

  public void setRole_alertspreference(int role_alertspreference) {
    this.role_alertspreference = role_alertspreference;
  }

  public boolean smslog() {
    return role_smslog == 1;
  }

  public void setRole_smslog(int role_smslog) {
    this.role_smslog = role_smslog;
  }

  public boolean phonerecord() {
    return role_phonerecord == 1;
  }

  public void setRole_phonerecord(int role_phonerecord) {
    this.role_phonerecord = role_phonerecord;
  }

  public boolean phonelog() {
    return role_phonelog == 1;
  }

  public void setRole_phonelog(int role_phonelog) {
    this.role_phonelog = role_phonelog;
  }


  public boolean videocases() {
    return role_videocases == 1;
  }

  public void setRole_videocases(int role_videocases) {
    this.role_videocases = role_videocases;
  }

  public boolean videostreaming() {
    return role_videostreaming == 1;
  }

  public void setRole_videostreaming(int role_videostreaming) {
    this.role_videostreaming = role_videostreaming;
  }

  public boolean broadcastingservice() {
    return role_broadcastingservice == 1;
  }

  public void setRole_broadcastingservice(int role_broadcastingservice) {
    this.role_broadcastingservice = role_broadcastingservice;
  }

  public boolean panicovert() {
    return role_panicovert == 1;
  }

  public void setRole_panicovert(int role_panicovert) {
    this.role_panicovert = role_panicovert;
  }

  public boolean collect() {
    return this.role_audiorecord == 1 || this.role_audiostream == 1 || this.role_existingmedia == 1
        || this.role_photocollect == 1 || this.role_videorecord == 1 || this.role_videostream == 1;
  }

  public boolean preferences() {
    return bossmodepref() || cases() || defaultscreen() || livetracking() || photos() || security()
        || SMS_Fixes() || SMS_SOS() || videocases() || videostreaming();
  }

  public boolean help() {
    return role_help == 1;
  }

  public void setRole_help(int role_help) {
    this.role_help = role_help;
  }

  // added 12/27/2016
  public boolean panicvolumebuttons() {
    return role_panicvolumebuttons == 1;
  }

  public void setRole_panicvolumebuttons(int role_panicvolumebuttons) {
    this.role_panicvolumebuttons = role_panicvolumebuttons;
  }

  public boolean task() {
    return role_task == 1;
  }

  public void setRole_task(int role_task) {
    this.role_task = role_task;
  }

  public boolean home2() {
    return role_home2 == 1;
  }

  public void setRole_home2(int role_home2) {
    this.role_home2 = role_home2;
  }

  public boolean overview() {
    return role_overview == 1;
  }

  public void setRole_overview(int role_overview) {
    this.role_overview = role_overview;
  }

  public boolean buttonnonemergency() {
    return role_buttonnonemergency == 1;
  }

  public void setRole_buttonnonemergency(int role_buttonnonemergency) {
    this.role_buttonnonemergency = role_buttonnonemergency;
  }

  public boolean buttonemergency() {
    return role_buttonemergency == 1;
  }

  public void setRole_buttonemergency(int role_buttonemergency) {
    this.role_buttonemergency = role_buttonemergency;
  }

  public boolean buttonpanic() {
    return role_buttonpanic == 1;
  }

  public void setRole_buttonpanic(int role_buttonpanic) {
    this.role_buttonpanic = role_buttonpanic;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AgentRoles that = (AgentRoles) o;

    if (role_alert != that.role_alert) {
      return false;
    }
    if (role_alertacceptance != that.role_alertacceptance) {
      return false;
    }
    if (role_alertspreference != that.role_alertspreference) {
      return false;
    }
    if (role_audiorecord != that.role_audiorecord) {
      return false;
    }
    if (role_audiostream != that.role_audiostream) {
      return false;
    }
    if (role_bossmode != that.role_bossmode) {
      return false;
    }
    if (role_bossmodeblack != that.role_bossmodeblack) {
      return false;
    }
    if (role_bossmodepref != that.role_bossmodepref) {
      return false;
    }
    if (role_broadcastingservice != that.role_broadcastingservice) {
      return false;
    }
    if (role_buttonemergency != that.role_buttonemergency) {
      return false;
    }
    if (role_buttonnonemergency != that.role_buttonnonemergency) {
      return false;
    }
    if (role_buttonpanic != that.role_buttonpanic) {
      return false;
    }
    if (role_cases != that.role_cases) {
      return false;
    }
    if (role_chat != that.role_chat) {
      return false;
    }
    if (role_checkin != that.role_checkin) {
      return false;
    }
    if (role_collectedmedia != that.role_collectedmedia) {
      return false;
    }
    if (role_contacts != that.role_contacts) {
      return false;
    }
    if (role_defaultscreen != that.role_defaultscreen) {
      return false;
    }
    if (role_existingmedia != that.role_existingmedia) {
      return false;
    }
    if (role_exitbossmode != that.role_exitbossmode) {
      return false;
    }
    if (role_factoryreset != that.role_factoryreset) {
      return false;
    }
    if (role_help != that.role_help) {
      return false;
    }
    if (role_home != that.role_home) {
      return false;
    }
    if (role_home2 != that.role_home2) {
      return false;
    }
    if (role_livetracking != that.role_livetracking) {
      return false;
    }
    if (role_livetrackingon != that.role_livetrackingon) {
      return false;
    }
    if (role_map != that.role_map) {
      return false;
    }
    if (role_mapcapture != that.role_mapcapture) {
      return false;
    }
    if (role_overview != that.role_overview) {
      return false;
    }
    if (role_panic != that.role_panic) {
      return false;
    }
    if (role_paniccovert != that.role_paniccovert) {
      return false;
    }
    if (role_paniccovertheadphones != that.role_paniccovertheadphones) {
      return false;
    }
    if (role_pointsofinterest != that.role_pointsofinterest) {
      return false;
    }
    if (role_panicovert != that.role_panicovert) {
      return false;
    }
    if (role_panictimed != that.role_panictimed) {
      return false;
    }
    if (role_panicvolumebuttons != that.role_panicvolumebuttons) {
      return false;
    }
    if (role_phonecollect != that.role_phonecollect) {
      return false;
    }
    if (role_phonelog != that.role_phonelog) {
      return false;
    }
    if (role_phonerecord != that.role_phonerecord) {
      return false;
    }
    if (role_photocollect != that.role_photocollect) {
      return false;
    }
    if (role_photos != that.role_photos) {
      return false;
    }
    if (role_security != that.role_security) {
      return false;
    }
    if (role_smslog != that.role_smslog) {
      return false;
    }
    if (role_SMS_Fixes != that.role_SMS_Fixes) {
      return false;
    }
    if (role_SMS_SOS != that.role_SMS_SOS) {
      return false;
    }
    if (role_task != that.role_task) {
      return false;
    }
    if (role_videocases != that.role_videocases) {
      return false;
    }
    if (role_videorecord != that.role_videorecord) {
      return false;
    }
    if (role_videostream != that.role_videostream) {
      return false;
    }
    if (role_videostreaming != that.role_videostreaming) {
      return false;
    }
    if(role_g4_at_commands!= that.role_g4_at_commands ){
      return false;
    }
    if(role_g4_mark_alert != that.role_g4_mark_alert){
      return false;
    }
    if(role_g4_configuration!= that.role_g4_configuration){
      return false;
    }
    if(role_g4_status!= that.role_g4_status){
      return false;
    }
    if(role_g4_messenger!=that.role_g4_messenger){
      return false;
    }

    return role_wipedata == that.role_wipedata;
  }

  @Override
  public int hashCode() {
    int result = role_alert;
    result = 31 * result + role_alertacceptance;
    result = 31 * result + role_alertspreference;
    result = 31 * result + role_audiorecord;
    result = 31 * result + role_audiostream;
    result = 31 * result + role_bossmode;
    result = 31 * result + role_bossmodeblack;
    result = 31 * result + role_bossmodepref;
    result = 31 * result + role_broadcastingservice;
    result = 31 * result + role_buttonemergency;
    result = 31 * result + role_buttonnonemergency;
    result = 31 * result + role_buttonpanic;
    result = 31 * result + role_cases;
    result = 31 * result + role_chat;
    result = 31 * result + role_checkin;
    result = 31 * result + role_collectedmedia;
    result = 31 * result + role_contacts;
    result = 31 * result + role_defaultscreen;
    result = 31 * result + role_existingmedia;
    result = 31 * result + role_exitbossmode;
    result = 31 * result + role_factoryreset;
    result = 31 * result + role_help;
    result = 31 * result + role_home;
    result = 31 * result + role_home2;
    result = 31 * result + role_livetracking;
    result = 31 * result + role_livetrackingon;
    result = 31 * result + role_map;
    result = 31 * result + role_mapcapture;
    result = 31 * result + role_overview;
    result = 31 * result + role_panic;
    result = 31 * result + role_paniccovert;
    result = 31 * result + role_paniccovertheadphones;
    result = 31 * result + role_pointsofinterest;
    result = 31 * result + role_panicovert;
    result = 31 * result + role_panictimed;
    result = 31 * result + role_panicvolumebuttons;
    result = 31 * result + role_phonecollect;
    result = 31 * result + role_phonelog;
    result = 31 * result + role_phonerecord;
    result = 31 * result + role_photocollect;
    result = 31 * result + role_photos;
    result = 31 * result + role_security;
    result = 31 * result + role_smslog;
    result = 31 * result + role_SMS_Fixes;
    result = 31 * result + role_SMS_SOS;
    result = 31 * result + role_task;
    result = 31 * result + role_videocases;
    result = 31 * result + role_videorecord;
    result = 31 * result + role_videostream;
    result = 31 * result + role_videostreaming;
    result = 31 * result + role_wipedata;
    result = 31 * result + role_g4_status;
    result = 31 * result + role_g4_mark_alert;
    result = 31 * result + role_g4_configuration;
    result = 31 * result + role_g4_messenger;
    result = 31 * result + role_g4_at_commands;
    return result;
  }

  @Override
  public String toString() {
    return "AgentRoles{" +
        "role_alert=" + role_alert +
        ", role_alertacceptance=" + role_alertacceptance +
        ", role_alertspreference=" + role_alertspreference +
        ", role_audiorecord=" + role_audiorecord +
        ", role_audiostream=" + role_audiostream +
        ", role_bossmode=" + role_bossmode +
        ", role_bossmodeblack=" + role_bossmodeblack +
        ", role_bossmodepref=" + role_bossmodepref +
        ", role_broadcastingservice=" + role_broadcastingservice +
        ", role_buttonemergency=" + role_buttonemergency +
        ", role_buttonnonemergency=" + role_buttonnonemergency +
        ", role_buttonpanic=" + role_buttonpanic +
        ", role_cases=" + role_cases +
        ", role_chat=" + role_chat +
        ", role_checkin=" + role_checkin +
        ", role_collectedmedia=" + role_collectedmedia +
        ", role_contacts=" + role_contacts +
        ", role_defaultscreen=" + role_defaultscreen +
        ", role_existingmedia=" + role_existingmedia +
        ", role_exitbossmode=" + role_exitbossmode +
        ", role_factoryreset=" + role_factoryreset +
        ", role_help=" + role_help +
        ", role_home=" + role_home +
        ", role_home2=" + role_home2 +
        ", role_livetracking=" + role_livetracking +
        ", role_livetrackingon=" + role_livetrackingon +
        ", role_map=" + role_map +
        ", role_mapcapture=" + role_mapcapture +
        ", role_overview=" + role_overview +
        ", role_panic=" + role_panic +
        ", role_paniccovert=" + role_paniccovert +
        ", role_paniccovertheadphones=" + role_paniccovertheadphones +
        ", role_pointsofinterest=" + role_pointsofinterest +
        ", role_panicovert=" + role_panicovert +
        ", role_panictimed=" + role_panictimed +
        ", role_panicvolumebuttons=" + role_panicvolumebuttons +
        ", role_phonecollect=" + role_phonecollect +
        ", role_phonelog=" + role_phonelog +
        ", role_phonerecord=" + role_phonerecord +
        ", role_photocollect=" + role_photocollect +
        ", role_photos=" + role_photos +
        ", role_security=" + role_security +
        ", role_smslog=" + role_smslog +
        ", role_SMS_Fixes=" + role_SMS_Fixes +
        ", role_SMS_SOS=" + role_SMS_SOS +
        ", role_task=" + role_task +
        ", role_videocases=" + role_videocases +
        ", role_videorecord=" + role_videorecord +
        ", role_videostream=" + role_videostream +
        ", role_videostreaming=" + role_videostreaming +
        ", role_wipedata=" + role_wipedata +
        ", audiorecord=" + audiorecord() +
        ", audiostream=" + audiostream() +
        ", existingmedia=" + existingmedia() +
        ", photocollect=" + photocollect() +
        ", phonecollect=" + phonecollect() +
        ", videorecord=" + videorecord() +
        ", videostream=" + videostream() +
        ", alert=" + alert() +
        ", bossmode=" + bossmode() +
        ", bossmodeblack=" + bossmodeblack() +
        ", chat=" + chat() +
        ", home=" + home() +
        ", collectedmedia=" + collectedmedia() +
        ", map=" + map() +
        ", paniccovert=" + paniccovert() +
        ", pointsofinterest=" + pointsofinterest() +
        ", wipedata=" + wipedata() +
        ", paniccovertheadphones=" + paniccovertheadphones() +
        ", bossmodepref=" + bossmodepref() +
        ", cases=" + cases() +
        ", defaultscreen=" + defaultscreen() +
        ", livetracking=" + livetracking() +
        ", exitbossmode=" + exitbossmode() +
        ", livetrackingon=" + livetrackingon() +
        ", photos=" + photos() +
        ", security=" + security() +
        ", SMS_Fixes=" + SMS_Fixes() +
        ", SMS_SOS=" + SMS_SOS() +
        ", alertacceptance=" + alertacceptance() +
        ", factoryreset=" + factoryreset() +
        ", contacts=" + contacts() +
        ", checkin=" + checkin() +
        ", panictimed=" + panictimed() +
        ", mapcapture=" + mapcapture() +
        ", panic=" + panic() +
        ", alertspreference=" + alertspreference() +
        ", smslog=" + smslog() +
        ", phonerecord=" + phonerecord() +
        ", phonelog=" + phonelog() +
        ", videocases=" + videocases() +
        ", videostreaming=" + videostreaming() +
        ", broadcastingservice=" + broadcastingservice() +
        ", panicovert=" + panicovert() +
        ", collect=" + collect() +
        ", preferences=" + preferences() +
        ", help=" + help() +
        ", panicvolumebuttons=" + panicvolumebuttons() +
        ", task=" + task() +
        ", home2=" + home2() +
        ", overview=" + overview() +
        ", buttonnonemergency=" + buttonnonemergency() +
        ", buttonemergency=" + buttonemergency() +
        ", buttonpanic=" + buttonpanic() +
        ",role_g4_status="+ role_g4_status +
        ",role_g4_mark_alert="+ role_g4_mark_alert +
        ",role_g4_configuration="+role_g4_configuration+
        ",role_g4_messenger="+role_g4_messenger+
        ",role_g4_at_commands="+role_g4_at_commands+
        '}';
  }


  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(this.role_alert);
    dest.writeInt(this.role_alertacceptance);
    dest.writeInt(this.role_alertspreference);
    dest.writeInt(this.role_audiorecord);
    dest.writeInt(this.role_audiostream);
    dest.writeInt(this.role_bossmode);
    dest.writeInt(this.role_bossmodeblack);
    dest.writeInt(this.role_bossmodepref);
    dest.writeInt(this.role_broadcastingservice);
    dest.writeInt(this.role_buttonemergency);
    dest.writeInt(this.role_buttonnonemergency);
    dest.writeInt(this.role_buttonpanic);
    dest.writeInt(this.role_cases);
    dest.writeInt(this.role_chat);
    dest.writeInt(this.role_checkin);
    dest.writeInt(this.role_collectedmedia);
    dest.writeInt(this.role_contacts);
    dest.writeInt(this.role_defaultscreen);
    dest.writeInt(this.role_existingmedia);
    dest.writeInt(this.role_exitbossmode);
    dest.writeInt(this.role_factoryreset);
    dest.writeInt(this.role_help);
    dest.writeInt(this.role_home);
    dest.writeInt(this.role_home2);
    dest.writeInt(this.role_livetracking);
    dest.writeInt(this.role_livetrackingon);
    dest.writeInt(this.role_map);
    dest.writeInt(this.role_mapcapture);
    dest.writeInt(this.role_overview);
    dest.writeInt(this.role_panic);
    dest.writeInt(this.role_paniccovert);
    dest.writeInt(this.role_paniccovertheadphones);
    dest.writeInt(this.role_pointsofinterest);
    dest.writeInt(this.role_panicovert);
    dest.writeInt(this.role_panictimed);
    dest.writeInt(this.role_panicvolumebuttons);
    dest.writeInt(this.role_phonecollect);
    dest.writeInt(this.role_phonelog);
    dest.writeInt(this.role_phonerecord);
    dest.writeInt(this.role_photocollect);
    dest.writeInt(this.role_photos);
    dest.writeInt(this.role_security);
    dest.writeInt(this.role_smslog);
    dest.writeInt(this.role_SMS_Fixes);
    dest.writeInt(this.role_SMS_SOS);
    dest.writeInt(this.role_task);
    dest.writeInt(this.role_videocases);
    dest.writeInt(this.role_videorecord);
    dest.writeInt(this.role_videostream);
    dest.writeInt(this.role_videostreaming);
    dest.writeInt(this.role_wipedata);
    dest.writeInt(this.role_g4_at_commands);
    dest.writeInt(this.role_g4_configuration);
    dest.writeInt(this.role_g4_mark_alert);
    dest.writeInt(this.role_g4_messenger);
    dest.writeInt(this.role_g4_status);
  }

  protected AgentRoles(Parcel in) {
    this.role_alert = in.readInt();
    this.role_alertacceptance = in.readInt();
    this.role_alertspreference = in.readInt();
    this.role_audiorecord = in.readInt();
    this.role_audiostream = in.readInt();
    this.role_bossmode = in.readInt();
    this.role_bossmodeblack = in.readInt();
    this.role_bossmodepref = in.readInt();
    this.role_broadcastingservice = in.readInt();
    this.role_buttonemergency = in.readInt();
    this.role_buttonnonemergency = in.readInt();
    this.role_buttonpanic = in.readInt();
    this.role_cases = in.readInt();
    this.role_chat = in.readInt();
    this.role_checkin = in.readInt();
    this.role_collectedmedia = in.readInt();
    this.role_contacts = in.readInt();
    this.role_defaultscreen = in.readInt();
    this.role_existingmedia = in.readInt();
    this.role_exitbossmode = in.readInt();
    this.role_factoryreset = in.readInt();
    this.role_help = in.readInt();
    this.role_home = in.readInt();
    this.role_home2 = in.readInt();
    this.role_livetracking = in.readInt();
    this.role_livetrackingon = in.readInt();
    this.role_map = in.readInt();
    this.role_mapcapture = in.readInt();
    this.role_overview = in.readInt();
    this.role_panic = in.readInt();
    this.role_paniccovert = in.readInt();
    this.role_paniccovertheadphones = in.readInt();
    this.role_pointsofinterest = in.readInt();
    this.role_panicovert = in.readInt();
    this.role_panictimed = in.readInt();
    this.role_panicvolumebuttons = in.readInt();
    this.role_phonecollect = in.readInt();
    this.role_phonelog = in.readInt();
    this.role_phonerecord = in.readInt();
    this.role_photocollect = in.readInt();
    this.role_photos = in.readInt();
    this.role_security = in.readInt();
    this.role_smslog = in.readInt();
    this.role_SMS_Fixes = in.readInt();
    this.role_SMS_SOS = in.readInt();
    this.role_task = in.readInt();
    this.role_videocases = in.readInt();
    this.role_videorecord = in.readInt();
    this.role_videostream = in.readInt();
    this.role_videostreaming = in.readInt();
    this.role_wipedata = in.readInt();
    this.role_g4_at_commands = in.readInt();
    this.role_g4_mark_alert = in.readInt();
    this.role_g4_status = in.readInt();
    this.role_g4_messenger = in.readInt();
    this.role_g4_configuration = in.readInt();
  }

  public static final Creator<AgentRoles> CREATOR = new Creator<AgentRoles>() {
    @Override
    public AgentRoles createFromParcel(Parcel source) {
      return new AgentRoles(source);
    }

    @Override
    public AgentRoles[] newArray(int size) {
      return new AgentRoles[size];
    }
  };
}
