package com.emazon.api_shopping_cart.domain.usecase;

import com.emazon.api_shopping_cart.domain.exception.CategoryLimitException;
import com.emazon.api_shopping_cart.domain.exception.TheItemIsNotAvailable;
import com.emazon.api_shopping_cart.domain.model.CartSave;
import com.emazon.api_shopping_cart.domain.model.stock.ArticleResponse;
import com.emazon.api_shopping_cart.domain.model.stock.CategoryResponseList;
import com.emazon.api_shopping_cart.domain.spi.IAthenticationPersistencePort;
import com.emazon.api_shopping_cart.domain.spi.ICartPersistencePort;
import com.emazon.api_shopping_cart.domain.spi.ICartStockPersistencePort;
import com.emazon.api_shopping_cart.domain.util.ConstantsDomain;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

class CartUseCaseTest {

    @Mock
    private ICartPersistencePort cartPersistencePort;

    @Mock
    private IAthenticationPersistencePort authenticationPersistencePort;

    @Mock
    private ICartStockPersistencePort cartStockPersistencePort;

    @InjectMocks
    private CartUseCase cartUseCase;

    private CartSave cartRequest;
    private CartSave cartDataBase;
    private ArticleResponse articleResponse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        cartRequest = new CartSave();
        cartRequest.setIdArticle(ConstantsDomain.ID_ARTICLE);
        cartRequest.setQuantity(ConstantsDomain.QUANTITY);

        cartDataBase = new CartSave();
        cartDataBase.setIdArticle(ConstantsDomain.ID_ARTICLE);
        cartDataBase.setQuantity(ConstantsDomain.QUANTITY);

        articleResponse = new ArticleResponse();
        articleResponse.setQuantity(ConstantsDomain.QUANTITY);
        articleResponse.setCategories(new ArrayList<>());
    }

    @Test
    void testCartSaveNewArticle() {
        Mockito.when(authenticationPersistencePort.getUserName()).thenReturn(ConstantsDomain.EMAIL);
        Mockito.when(cartStockPersistencePort.existArticleById(cartRequest.getIdArticle()))
                .thenReturn(articleResponse);
        Mockito.when(cartPersistencePort.findCartByUserNameAndArticleId(cartRequest.getIdArticle(),
                ConstantsDomain.EMAIL)).thenReturn(null);

        cartUseCase.cartSave(cartRequest);

        Mockito.verify(cartPersistencePort).saveCart(Mockito.any(CartSave.class));
        Assertions.assertEquals(ConstantsDomain.EMAIL, cartRequest.getEmail());
        Assertions.assertNotNull(cartRequest.getIdArticle());
        Assertions.assertNotNull(cartRequest.getQuantity());
    }

    @Test
    void testCartSaveExistingArticleWithSufficientStock() {
        Mockito.when(authenticationPersistencePort.getUserName()).thenReturn(ConstantsDomain.EMAIL);
        Mockito.when(cartStockPersistencePort.existArticleById(ConstantsDomain.ID_ARTICLE))
                .thenReturn(articleResponse);
        Mockito.when(cartPersistencePort.findCartByUserNameAndArticleId(ConstantsDomain.ID_ARTICLE
                        , ConstantsDomain.EMAIL))
                .thenReturn(cartDataBase);

        cartUseCase.cartSave(cartRequest);

        Mockito.verify(cartPersistencePort).saveCart(Mockito.any(CartSave.class));
    }

    @Test
    void testCartSaveNotAvailableStock() {
        ArticleResponse articleResponseNotAvailable = new ArticleResponse();
        articleResponseNotAvailable.setQuantity(ConstantsDomain.NUMBER_0);

        Mockito.when(authenticationPersistencePort.getUserName()).thenReturn(ConstantsDomain.EMAIL);
        Mockito.when(cartStockPersistencePort.existArticleById(ConstantsDomain.ID_ARTICLE))
                .thenReturn(articleResponseNotAvailable);
        Mockito.when(cartPersistencePort.getNextDate()).thenReturn(LocalDate.now());

        Assertions.assertThrows(TheItemIsNotAvailable.class, () -> {
            cartUseCase.cartSave(cartRequest);
        });

        Mockito.verify(authenticationPersistencePort, Mockito.times(ConstantsDomain.NUMBER_1))
                .getUserName();
        Mockito.verify(cartStockPersistencePort, Mockito.times(ConstantsDomain.NUMBER_1))
                .existArticleById(ConstantsDomain.ID_ARTICLE);
    }

    @Test
    void testValidateCategoryLimitExceeded() {
        List<CategoryResponseList> categoryResponseLists = new ArrayList<>();
        CategoryResponseList categoryResponseList = new
                CategoryResponseList(ConstantsDomain.ID_ARTICLE,ConstantsDomain.DESCRIPTION);
        categoryResponseLists.add(categoryResponseList);
        categoryResponseLists.add(categoryResponseList);
        categoryResponseLists.add(categoryResponseList);
        categoryResponseLists.add(categoryResponseList);

        ArticleResponse articleResponseLimit = new ArticleResponse();
        articleResponseLimit.setQuantity(ConstantsDomain.QUANTITY);
        articleResponseLimit.setCategories(categoryResponseLists);

        Mockito.when(authenticationPersistencePort.getUserName()).thenReturn(ConstantsDomain.EMAIL);
        Mockito.when(cartStockPersistencePort.existArticleById(ConstantsDomain.ID_ARTICLE))
                .thenReturn(articleResponseLimit);
        Mockito.when(cartPersistencePort.findCartByUserNameAndArticleId(ConstantsDomain.ID_ARTICLE
                        , ConstantsDomain.EMAIL))
                .thenReturn(null);
        List<Integer> mockCart = new ArrayList<>(List.of(ConstantsDomain.ID_ARTICLE
                , ConstantsDomain.ID_ARTICLE, ConstantsDomain.ID_ARTICLE));

        Mockito.when(cartPersistencePort.findCartByUserName(ConstantsDomain.EMAIL)).thenReturn(mockCart);

        Assertions.assertThrows(CategoryLimitException.class, () -> {
            cartUseCase.cartSave(cartRequest);
        });

        Mockito.verify(cartPersistencePort, Mockito.times(ConstantsDomain.NUMBER_0))
                .saveCart(Mockito.any(CartSave.class));
        Mockito.verify(cartStockPersistencePort, Mockito.times(ConstantsDomain.NUMBER_2))
                .existArticleById(ConstantsDomain.ID_ARTICLE);
    }

    @Test
    void testValidateCategoryNotLimitExceeded() {
        Mockito.when(authenticationPersistencePort.getUserName()).thenReturn(ConstantsDomain.EMAIL);
        Mockito.when(cartStockPersistencePort.existArticleById(ConstantsDomain.ID_ARTICLE))
                .thenReturn(articleResponse);
        Mockito.when(cartPersistencePort.findCartByUserNameAndArticleId(ConstantsDomain.ID_ARTICLE
                        , ConstantsDomain.EMAIL))
                .thenReturn(null);
        List<Integer> mockCart = new ArrayList<>(List.of(ConstantsDomain.ID_ARTICLE
                , ConstantsDomain.ID_ARTICLE, ConstantsDomain.ID_ARTICLE));
        Mockito.when(cartPersistencePort.findCartByUserName(ConstantsDomain.EMAIL)).thenReturn(mockCart);

        cartUseCase.cartSave(cartRequest);


        Mockito.verify(cartPersistencePort, Mockito.times(ConstantsDomain.NUMBER_1))
                .saveCart(Mockito.any(CartSave.class));
        Mockito.verify(cartStockPersistencePort, Mockito.times(ConstantsDomain.NUMBER_5))
                .existArticleById(ConstantsDomain.ID_ARTICLE);
    }
}