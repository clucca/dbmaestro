--USE AH3111ODZ


CREATE TABLE ZZR_MSG (
	MSG_CD varchar2(5) NOT NULL,
	MSG_TXT varchar2(254) NOT NULL,
	MSG_SEV number NOT NULL,
	MSG_EXPL varchar2(1000) NULL,
	OV_LVL number NULL,
	AMS_ROW_VERS_NO bigint NULL,
	MSG_TXT_UP varchar2(254) NULL,
	TBL_LAST_DT timestamp NULL
);

