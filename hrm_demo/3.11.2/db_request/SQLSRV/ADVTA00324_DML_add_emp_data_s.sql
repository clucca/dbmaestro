--ADVHR00123429
--DML

update  R_MSG set MSG_TXT = 'Traveler cannot be on leave during the travel period',MSG_EXPL = 'The specified traveler must not be on leave during the trip''s start and begin dates. ',MSG_TXT_UP = 'TRAVELER CANNOT BE ON LEAVE DURING THE TRAVEL PERIOD',
MSG_SEV = 1 where MSG_CD = 'A7830'
GO

update R_MSG set MSG_TXT = 'Traveler must not be on leave during the travel period.',MSG_EXPL = 'The specified traveler must not be on leave during the trip''s start and begin dates.' , MSG_TXT_UP = 'TRAVELER MUST NOT BE ON LEAVE DURING THE TRAVEL PERIOD.',
MSG_SEV = 2 where MSG_CD = 'A7237'
GO


--ADVHR00123430
--DML


INSERT INTO R_MSG (MSG_CD,MSG_TXT,MSG_SEV,MSG_EXPL,OV_LVL,AMS_ROW_VERS_NO,MSG_TXT_UP,TBL_LAST_DT) VALUES ('A3449','A reason for modification must be entered on the document to record the reasoning behind revising the original entry.',2,'A reason for modification must be entered on the document to record the reasoning behind revising the original entry.',0,1,'A REASON FOR MODIFICATION MUST BE ENTERED ON THE DOCUMENT TO RECORD THE REASONING BEHIND REVISING THE ORIGINAL ENTRY.',NULL)

GO 

--ADVHR00123437
--DML

DELETE FROM R_MSG WHERE MSG_CD='M0096'
GO

INSERT INTO R_MSG( MSG_TXT, MSG_EXPL, MSG_CD, MSG_TXT_UP, AMS_ROW_VERS_NO, OV_LVL, MSG_SEV, TBL_LAST_DT ) values('Start Time must be after previous End Time.', 'Start Time for Shifts 2, 3 and 4 must be set to values after End Time for Shifts 1, 2 and 3 respectively.', 'M0096', 'START TIME MUST BE AFTER PREVIOUS END TIME.', 1, 0, 2, null )
GO



--ADVHR00123420
--DML

Update R_MSG
set MSG_EXPL = 'Total of Event List hours are not within the allowed threshold for the week (refer to the Short Message to identify the description of the events included in the Event List)'
where MSG_CD = 'O0282'
GO

Update R_MSG
set MSG_EXPL = 'Total of Event List hours are not within the allowed threshold for the pay period (refer to the Short Message to identify the description of the events included in the Event List)'
where MSG_CD = 'O0283'
GO

Update R_MSG
set MSG_EXPL = 'An event is entered when there is available leave balance in Event List (refer to the Short Message to identify the event entered on the timesheet and the description of the events included in the leave balance check)'
where MSG_CD = 'O0284'
GO

Update R_MSG
set MSG_EXPL = 'Validation for the combination of events entered (refer to the Short Message to identify the event entered on the timesheet and the description of the events included in the validation)'
where MSG_CD = 'O0285'
GO

Update R_MSG
set MSG_EXPL = 'Validation for the combination of events entered (refer to the Short Message to identify the event entered on the timesheet and the description of the events included in the validation)'
where MSG_CD = 'O0586'
GO

Update R_MSG
set MSG_EXPL = 'An event is entered on a date that is not within the list of specified dates  (refer to the Short Message to identify the event entered on the timesheet and the description of the dates included in the date check)'
where MSG_CD = 'O0286'
GO

Update R_MSG
set MSG_EXPL = 'The total of specified event hours is more than the list of events included for the check (refer to the Short Message to identify the event entered on the timesheet and the description of the events included in the total hours)'
where MSG_CD = 'O0287'
GO

Update R_MSG
set MSG_EXPL = 'An event is entered and the total timesheet hours for specific events entered over the specified number of pay periods is less than the hours required to be worked (refer to the Short Message to identify the event entered on the timesheet and the description of the events included in the past period hours calculation)'
where MSG_CD = 'O0288'
GO

EXIT;