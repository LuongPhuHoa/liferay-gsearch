
package fi.soveltia.liferay.gsearch.core.impl.results.item;

import com.liferay.asset.kernel.model.AssetRenderer;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.service.JournalArticleService;
import com.liferay.message.boards.kernel.model.MBMessage;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.portlet.LiferayPortletRequest;
import com.liferay.portal.kernel.portlet.LiferayPortletResponse;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.wiki.model.WikiPage;

import javax.portlet.WindowState;

import org.apache.commons.lang3.StringEscapeUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import fi.soveltia.liferay.gsearch.core.api.results.item.ResultItemBuilder;

/**
 * MB message result item builder.
 * 
 * @author Petteri Karttunen
 */
@Component(
	immediate = true,
	service = ResultItemBuilder.class
)
public class MBMessageItemBuilder extends BaseResultItemBuilder
	implements ResultItemBuilder {

	@Override
	public boolean canBuild(Document document) {

		return NAME.equals(document.get(Field.ENTRY_CLASS_NAME));	}

	/**
	 * This currently handles links for following MBMessage types (by message
	 * classNameId field) com.liferay.wiki.model.WikiPage
	 * com.liferay.journal.model.JournalArticle
	 */
	@Override
	public String getLink()
		throws Exception {

		long classNameId =
			GetterUtil.getLong(_document.get(Field.CLASS_NAME_ID));

		long classPK = GetterUtil.getLong(_document.get(Field.CLASS_PK));

		if (classNameId > 0) {

			String className = _portal.getClassName(classNameId);

			if (JournalArticle.class.getName().equals(className)) {
				return getJournalArticleCommentLink(classPK);
			}
			else if (WikiPage.class.getName().equals(className)) {
				return getWikiPageCommentLink(classPK);
			}
		}

		return getAssetRenderer().getURLViewInContext(
			(LiferayPortletRequest) _portletRequest,
			(LiferayPortletResponse) _portletResponse, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getTitle()
		throws NumberFormatException, PortalException {

		long classNameId =
			GetterUtil.getLong(_document.get(Field.CLASS_NAME_ID));

		if (classNameId > 0) {

			String className = _portal.getClassName(classNameId);

			if (JournalArticle.class.getName().equals(className) ||
				WikiPage.class.getName().equals(className)) {

				String title = _document.get(Field.CONTENT);

				if (title.length() > TITLE_MAXLENGTH) {
					title = title.substring(0, TITLE_MAXLENGTH) + "...";
				}

				// Using Apache commons as it works better than HTMLUtils

				title = StringEscapeUtils.unescapeHtml4(title);
				title = HtmlUtil.stripHtml(title);

				return title;
			}
		}
		return super.getTitle();
	}

	protected String getDLFileEntryCommentLink() {

		return null;
	}

	/**
	 * Get journal article link
	 * 
	 * @param classPK
	 * @return
	 * @throws Exception
	 */
	protected String getJournalArticleCommentLink(long classPK)
		throws Exception {

		AssetRenderer<?> assetRenderer =
			getAssetRenderer(JournalArticle.class.getName(), classPK);

		String link = assetRenderer.getURLViewInContext(
			(LiferayPortletRequest) _portletRequest,
			(LiferayPortletResponse) _portletResponse, null);

		if (Validator.isNull(link)) {

			JournalArticle journalArticle =
				_journalArticleService.getLatestArticle(classPK);
			link = getNotLayoutBoundJournalArticleUrl(journalArticle);
		}

		return link;
	}

	/**
	 * Get wiki comment link.
	 * 
	 * @return
	 * @throws Exception
	 */
	protected String getWikiPageCommentLink(long classPK)
		throws Exception {

		AssetRenderer<?> assetRenderer =
			getAssetRenderer(WikiPage.class.getName(), classPK);

		return assetRenderer.getURLView(
			(LiferayPortletResponse) _portletResponse, WindowState.MAXIMIZED);
	}

	@Reference(unbind = "-")
	protected void setJournalArticleService(
		JournalArticleService journalArticleService) {

		_journalArticleService = journalArticleService;
	}

	@Reference(unbind = "-")
	protected void setPortal(Portal portal) {

		_portal = portal;
	}

	private static JournalArticleService _journalArticleService;

	private static final String NAME = MBMessage.class.getName();

	private static final int TITLE_MAXLENGTH = 80;

	private Portal _portal;
}
