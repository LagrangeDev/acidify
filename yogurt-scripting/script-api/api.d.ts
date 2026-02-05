/* eslint-disable */
import * as types from '@saltify/milky-types';
import type z from 'zod';

export interface ApiCollection {
  // system API
  get_login_info: () => Promise<types.GetLoginInfoOutput>;
  get_impl_info: () => Promise<types.GetImplInfoOutput>;
  get_user_profile: (input: z.input<typeof types.GetUserProfileInput>) => Promise<types.GetUserProfileOutput>;
  get_friend_list: (input: z.input<typeof types.GetFriendListInput>) => Promise<types.GetFriendListOutput>;
  get_friend_info: (input: z.input<typeof types.GetFriendInfoInput>) => Promise<types.GetFriendInfoOutput>;
  get_group_list: (input: z.input<typeof types.GetGroupListInput>) => Promise<types.GetGroupListOutput>;
  get_group_info: (input: z.input<typeof types.GetGroupInfoInput>) => Promise<types.GetGroupInfoOutput>;
  get_group_member_list: (input: z.input<typeof types.GetGroupMemberListInput>) => Promise<types.GetGroupMemberListOutput>;
  get_group_member_info: (input: z.input<typeof types.GetGroupMemberInfoInput>) => Promise<types.GetGroupMemberInfoOutput>;
  set_avatar: (input: z.input<typeof types.SetAvatarInput>) => Promise<void>;
  set_nickname: (input: z.input<typeof types.SetNicknameInput>) => Promise<void>;
  set_bio: (input: z.input<typeof types.SetBioInput>) => Promise<void>;
  get_custom_face_url_list: () => Promise<types.GetCustomFaceUrlListOutput>;
  get_cookies: (input: z.input<typeof types.GetCookiesInput>) => Promise<types.GetCookiesOutput>;
  get_csrf_token: () => Promise<types.GetCSRFTokenOutput>;

  // message API
  send_private_message: (input: z.input<typeof types.SendPrivateMessageInput>) => Promise<types.SendPrivateMessageOutput>;
  send_group_message: (input: z.input<typeof types.SendGroupMessageInput>) => Promise<types.SendGroupMessageOutput>;
  recall_private_message: (input: z.input<typeof types.RecallPrivateMessageInput>) => Promise<void>;
  recall_group_message: (input: z.input<typeof types.RecallGroupMessageInput>) => Promise<void>;
  get_message: (input: z.input<typeof types.GetMessageInput>) => Promise<types.GetMessageOutput>;
  get_history_messages: (input: z.input<typeof types.GetHistoryMessagesInput>) => Promise<types.GetHistoryMessagesOutput>;
  get_resource_temp_url: (input: z.input<typeof types.GetResourceTempUrlInput>) => Promise<types.GetResourceTempUrlOutput>;
  get_forwarded_messages: (input: z.input<typeof types.GetForwardedMessagesInput>) => Promise<types.GetForwardedMessagesOutput>;
  mark_message_as_read: (input: z.input<typeof types.MarkMessageAsReadInput>) => Promise<void>;

  // friend API
  send_friend_nudge: (input: z.input<typeof types.SendFriendNudgeInput>) => Promise<void>;
  send_profile_like: (input: z.input<typeof types.SendProfileLikeInput>) => Promise<void>;
  delete_friend: (input: z.input<typeof types.DeleteFriendInput>) => Promise<void>;
  get_friend_requests: (input: z.input<typeof types.GetFriendRequestsInput>) => Promise<types.GetFriendRequestsOutput>;
  accept_friend_request: (input: z.input<typeof types.AcceptFriendRequestInput>) => Promise<void>;
  reject_friend_request: (input: z.input<typeof types.RejectFriendRequestInput>) => Promise<void>;

  // group API
  set_group_name: (input: z.input<typeof types.SetGroupNameInput>) => Promise<void>;
  set_group_avatar: (input: z.input<typeof types.SetGroupAvatarInput>) => Promise<void>;
  set_group_member_card: (input: z.input<typeof types.SetGroupMemberCardInput>) => Promise<void>;
  set_group_member_special_title: (input: z.input<typeof types.SetGroupMemberSpecialTitleInput>) => Promise<void>;
  set_group_member_admin: (input: z.input<typeof types.SetGroupMemberAdminInput>) => Promise<void>;
  set_group_member_mute: (input: z.input<typeof types.SetGroupMemberMuteInput>) => Promise<void>;
  set_group_whole_mute: (input: z.input<typeof types.SetGroupWholeMuteInput>) => Promise<void>;
  kick_group_member: (input: z.input<typeof types.KickGroupMemberInput>) => Promise<void>;
  get_group_announcements: (input: z.input<typeof types.GetGroupAnnouncementsInput>) => Promise<types.GetGroupAnnouncementsOutput>;
  send_group_announcement: (input: z.input<typeof types.SendGroupAnnouncementInput>) => Promise<void>;
  delete_group_announcement: (input: z.input<typeof types.DeleteGroupAnnouncementInput>) => Promise<void>;
  get_group_essence_messages: (input: z.input<typeof types.GetGroupEssenceMessagesInput>) => Promise<types.GetGroupEssenceMessagesOutput>;
  set_group_essence_message: (input: z.input<typeof types.SetGroupEssenceMessageInput>) => Promise<void>;
  quit_group: (input: z.input<typeof types.QuitGroupInput>) => Promise<void>;
  send_group_message_reaction: (input: z.input<typeof types.SendGroupMessageReactionInput>) => Promise<void>;
  send_group_nudge: (input: z.input<typeof types.SendGroupNudgeInput>) => Promise<void>;
  get_group_notifications: (input: z.input<typeof types.GetGroupNotificationsInput>) => Promise<types.GetGroupNotificationsOutput>;
  accept_group_request: (input: z.input<typeof types.AcceptGroupRequestInput>) => Promise<void>;
  reject_group_request: (input: z.input<typeof types.RejectGroupRequestInput>) => Promise<void>;
  accept_group_invitation: (input: z.input<typeof types.AcceptGroupInvitationInput>) => Promise<void>;
  reject_group_invitation: (input: z.input<typeof types.RejectGroupInvitationInput>) => Promise<void>;

  // file API
  upload_private_file: (input: z.input<typeof types.UploadPrivateFileInput>) => Promise<types.UploadPrivateFileOutput>;
  upload_group_file: (input: z.input<typeof types.UploadGroupFileInput>) => Promise<types.UploadGroupFileOutput>;
  get_private_file_download_url: (input: z.input<typeof types.GetPrivateFileDownloadUrlInput>) => Promise<types.GetPrivateFileDownloadUrlOutput>;
  get_group_file_download_url: (input: z.input<typeof types.GetGroupFileDownloadUrlInput>) => Promise<types.GetGroupFileDownloadUrlOutput>;
  get_group_files: (input: z.input<typeof types.GetGroupFilesInput>) => Promise<types.GetGroupFilesOutput>;
  move_group_file: (input: z.input<typeof types.MoveGroupFileInput>) => Promise<void>;
  rename_group_file: (input: z.input<typeof types.RenameGroupFileInput>) => Promise<void>;
  delete_group_file: (input: z.input<typeof types.DeleteGroupFileInput>) => Promise<void>;
  create_group_folder: (input: z.input<typeof types.CreateGroupFolderInput>) => Promise<types.CreateGroupFolderOutput>;
  rename_group_folder: (input: z.input<typeof types.RenameGroupFolderInput>) => Promise<void>;
  delete_group_folder: (input: z.input<typeof types.DeleteGroupFolderInput>) => Promise<void>;

}